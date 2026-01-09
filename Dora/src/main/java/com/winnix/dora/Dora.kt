package com.winnix.dora

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.winnix.dora.admob_manager.AdmobNative
import com.winnix.dora.admob_manager.NativeLayout
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.LoadNativeCallback
import com.winnix.dora.callback.OpenAdCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.helper.UMPHelper
import com.winnix.dora.manager.BannerManager
import com.winnix.dora.manager.InterstitialManager
import com.winnix.dora.manager.NativeManager
import com.winnix.dora.manager.OpenAdManager
import com.winnix.dora.model.AdState
import com.winnix.dora.model.AdmobBannerSize
import com.winnix.dora.model.NativeResult
import com.winnix.dora.model.NativeType
import com.winnix.dora.ui.LoadingAdDialogFragment
import com.winnix.dora.ui.NativeFullDialog
import com.winnix.dora.yandex_manager.YandexAd
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

object Dora {
    private const val TAG = "Dora"
    private const val INTERSTITIAL_TIME_OUT = 6000L
    private val initBarrier = CompletableDeferred<Unit>()
    private var openAdManager: OpenAdManager? = null
    private var adState = AdState()
    private var yandexAd = YandexAd()
    private var lastInterstitialId: String? = null
    private var lastNativeFullId: String? = null
    private var lastInterstitialCallback: LoadInterstitialCallback? = null


    // General
    fun initializeAdmob(
        activity: Activity,
        debugId: String? = null
    ) {
        Log.d(TAG, "initAd SDK")

        if (initBarrier.isCompleted) {
            return
        }

        activity.window.decorView.post {
            UMPHelper.fetchConsent(
                activity,
                debugId
            ) { isSuccess ->
                val context = activity.application
                if (isSuccess) {
                    Log.d(TAG, "initialize: Khởi tạo Consent thành công")
                    CoroutineScope(Dispatchers.IO).launch {
                        MobileAds.initialize(context) {
                            completeInitAd()
                        }
                    }
                }
                else {
                    Log.w(TAG, "initialize: Khởi tạo Consent thất bại")
                }
            }
        }
    }

    private fun completeInitAd() {
        Log.d(TAG, "completeInitAd")
        initBarrier.complete(Unit)
    }

    internal suspend fun ensureInitialized() {
        initBarrier.await()
    }

    fun setUpYandex(
        intersUnit: String?,
        nativeUnit: String?,
        bannerUnit: String?,
    ) {
        yandexAd.intersUnit = intersUnit
        yandexAd.nativeUnit = nativeUnit
        yandexAd.bannerUnit = bannerUnit

        NativeManager.yandexId = nativeUnit
    }

    // Inters
    fun loadInterstitial(
        context: Context?,
        id: String,
        nativeFullId: String?,
        listener: LoadInterstitialCallback
    ) {
        if (context == null) return

        lastInterstitialId = id
        lastInterstitialCallback = listener

        InterstitialManager.loadInter(
            context = context,
            id = id,
            yandexId = yandexAd.intersUnit,
            listener
        )
        nativeFullId?.let {
            lastNativeFullId = it
            AdmobNative.loadAd(
                context = context,
                id = nativeFullId,
                nativeType = NativeType.NATIVE_FULL
            )
        }

    }

    fun showInterstitial(
        activity: AppCompatActivity?,
        timeout: Long = INTERSTITIAL_TIME_OUT,
        callback: ShowInterstitialCallback
    ) {
        if (activity == null || activity.isDestroyed || activity.isFinishing) {
            callback.onDismiss()
            return
        }

        if (!InterstitialManager.isAdmobAlready()) {
            lastInterstitialId?.let {
                loadInterstitial(
                    context = activity,
                    id = it,
                    nativeFullId = lastNativeFullId,
                    listener = lastInterstitialCallback ?: object : LoadInterstitialCallback{ }
                )
            }

            showLoadingDialog(activity)
        }

        var nativeAdJob: Job? = null
        val isHandled = AtomicBoolean(false)

        InterstitialManager.showInter(
            activity = activity,
            timeoutLong = timeout,
            callback = object : ShowInterstitialCallback {
                override fun onDismiss() {
                    adState = adState.copy(
                        isIntersShowing = false
                    )

                    if (isHandled.compareAndSet(false, true)) {
                        nativeAdJob?.cancel()

                        callback.onDismiss()
                    }
                }

                override fun onImpression() {
                    callback.onImpression()
                }

                override fun onShow() {
                    hideLoadingDialog(activity)

                    adState = adState.copy(
                        isIntersShowing = true
                    )

                    nativeAdJob = activity.lifecycleScope.launch(Dispatchers.Main) {
                        NativeManager.getAdFullState().first { state ->
                            if (state is NativeResult.Success) {
                                if (canShowDialog(activity, NativeFullDialog.TAG)) {
                                    if (isHandled.compareAndSet(false, true)) {
                                        val dialog = NativeFullDialog.newInstance(state.ad) {
                                            adState = adState.copy(
                                                isNativeFullShowing = false,
                                            )
                                            callback.onDismiss()
                                        }

                                        adState = adState.copy(
                                            isNativeFullShowing = true
                                        )

                                        NativeManager.clearNative(NativeType.NATIVE_FULL)

                                        dialog.show(
                                            activity.supportFragmentManager,
                                            NativeFullDialog.TAG
                                        )

                                    }
                                }
                            }
                            state is NativeResult.Success
                        }
                    }

                    callback.onShow()

                }

                override fun onShowFailed() {
                    hideLoadingDialog(activity)
                    nativeAdJob?.cancel()

                    adState = adState.copy(
                        isIntersShowing = false
                    )

                    callback.onShowFailed()
                }

            }
        )
    }

    fun waitForInterstitial(
        activity: AppCompatActivity?,
        timeout: Long,
        callback: LoadInterstitialCallback
    ) {
        activity?.lifecycleScope?.launch {
            val result = InterstitialManager.waitForInterstitialWithTimeout(timeout)
            if (result) {
                callback.onLoaded()
            } else {
                callback.onFailed()
            }
        }
    }

    private fun showLoadingDialog(
        activity: AppCompatActivity
    ) {
        if (activity.supportFragmentManager.isStateSaved) {
            return
        }

        if (activity.supportFragmentManager.findFragmentByTag(LoadingAdDialogFragment.TAG) == null) {
            LoadingAdDialogFragment().show(
                activity.supportFragmentManager,
                LoadingAdDialogFragment.TAG
            )
        }
    }

    private fun hideLoadingDialog(
        activity: AppCompatActivity
    ) {

        val dialog = activity.supportFragmentManager.findFragmentByTag(LoadingAdDialogFragment.TAG)
        if (dialog is LoadingAdDialogFragment) {
            dialog.dismissAllowingStateLoss()
        }
    }

    private fun canShowDialog(activity: AppCompatActivity, tag: String): Boolean {
        if (activity.supportFragmentManager.isStateSaved) {
            return false
        }

        return activity.supportFragmentManager.findFragmentByTag(tag) == null
    }

    // Native
    fun loadAndShowNative(
        activity: Activity,
        id: String,
        lifecycleOwner: LifecycleOwner,
        layout: NativeLayout,
        viewGroup: ViewGroup,
        callback: LoadNativeCallback? = null
    ) {
        NativeManager.loadAndShowAd(
            activity = activity,
            id = id,
            lifecycleOwner = lifecycleOwner,
            nativeType = NativeType.NATIVE,
            layoutAd = layout,
            viewGroup = viewGroup,
            callback = callback
        )
    }

    fun loadNative(
        context: Context,
        id: String
    ) {
        NativeManager.loadAd(
            context = context,
            id = id,
            nativeType = NativeType.NATIVE,
        )
    }

    // Banner
    @RequiresPermission(Manifest.permission.INTERNET)
    fun loadBanner(
        activity: Activity,
        container: ViewGroup,
        adSize: AdmobBannerSize,
        lifecycleOwner: LifecycleOwner,
        adUnitId: String
    ) {
        BannerManager.loadBanner(
            activity = activity,
            container = container,
            adSize = adSize,
            lifecycleOwner = lifecycleOwner,
            admobId = adUnitId,
            yandexId = yandexAd.bannerUnit
        )
    }

    //OpenAd
    fun registerOpenAd(
        application: Application,
        id: String,
        yandexId: String? = null,
        canShowOpenApp: () -> Boolean = { true }
    ) {
        openAdManager = OpenAdManager(
            id = id,
            application = application,
            yandexId = yandexId,
            callback = object : OpenAdCallback {
                override fun canShow(): () -> Boolean {
                    return {
                        Log.d(TAG, "isShowingAdFullscreen=${adState.isShowingAdFullscreen} canShowOpenApp=${canShowOpenApp()}")
                        !adState.isShowingAdFullscreen && canShowOpenApp()
                    }
                }

                override fun onShowAd() {
                    adState = adState.copy(
                        isOpenAppShowing = true
                    )
                }

                override fun onDismiss() {
                    adState = adState.copy(
                        isOpenAppShowing = false
                    )
                }

                override fun canLoad(): () -> Boolean {
                    return { true }
                }

            }
        )
    }

    fun showOpenAd(
        timeout: Long,
        activity: Activity,
        onDismiss: () -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            var isHandled = false

            withTimeoutOrNull(timeout) {
                while (true) {
                    if (openAdManager?.isAvailable() == true) {
                        isHandled = true
                        openAdManager?.showAdIfAvailable(activity, onDismiss)
                        break
                    }
                    delay(400)
                }
            }

            if (!isHandled) {
                onDismiss()
            }
        }
    }

    fun getOpenAppState() = openAdManager?.showState

    fun isPrivacyOptionsRequired(activity: Activity): Boolean =
        UMPHelper.isPrivacyOptionsRequired(activity)

    fun showPrivacyOptionsForm(activity: Activity, onReturn: (Boolean) -> Unit) =
        UMPHelper.showPrivacyOptionsForm(activity, onReturn)

    fun canRequestAdmob(activity: Activity) = UMPHelper.canRequestAds(activity)
}