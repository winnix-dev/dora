package com.winnix.dora

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.winnix.dora.callback.OpenAdCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.helper.InterstitialManager
import com.winnix.dora.helper.NativeLayout
import com.winnix.dora.helper.NativeManager
import com.winnix.dora.helper.OpenAdManager
import com.winnix.dora.helper.UMPHelper
import com.winnix.dora.model.AdConfig
import com.winnix.dora.model.AdState
import com.winnix.dora.model.AdmobBannerSize
import com.winnix.dora.model.AdmobUnit
import com.winnix.dora.ui.LoadingAdDialogFragment
import com.winnix.dora.ui.NativeFullDialog
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
    var isInitialized = false

    internal var applicationContext: Context? = null

    private val initBarrier = CompletableDeferred<Unit>()

    private var adConfig = AdConfig()

    private var interstitialManager = InterstitialManager()

    private var nativeManager = NativeManager()

    private var openAdManager : OpenAdManager? = null

    private var adState = AdState()

    // General
    fun initialize(
        activity: Activity,
        adConfig: AdConfig
    ) {
        Log.d(TAG, "initAd SDK")

        this.adConfig = adConfig
        applicationContext = activity.applicationContext

        if (initBarrier.isCompleted) {
            return
        }

        activity.window.decorView.post {
            UMPHelper.fetchConsent(
                activity
            ) { isSuccess ->
                val context = activity.application
                if (isSuccess) {
                    CoroutineScope(Dispatchers.IO).launch {
                        MobileAds.initialize(context) {
                            completeInitAd()
                        }
                    }
                } else {
                    completeInitAd()
                }
            }
        }
    }
    private fun completeInitAd() {
        Log.d(TAG, "completeInitAd")
        isInitialized = true
        initBarrier.complete(Unit)
    }
    private suspend fun ensureInitialized() {
        initBarrier.await()
    }

    private fun getAdId(adUnit: AdmobUnit) : String {
        return if (adConfig.isDebug) {
            adUnit.adType.getDebugId()
        } else {
            adUnit.id
        }
    }

    // Inters
    fun loadInterstitial(
        adUnit: AdmobUnit,
    ) {
        if(applicationContext == null) return

        CoroutineScope(Dispatchers.Main).launch {
            ensureInitialized()

            if (adConfig.admobGuard?.canLoadAd(adUnit) == false) {
                return@launch
            }

            interstitialManager.loadInterstitial(
                applicationContext?:return@launch,
                getAdId(adUnit),
            )
        }
    }

    fun showInterstitial(
        activity: AppCompatActivity?,
        adUnit: AdmobUnit,
        reloadAdUnit: AdmobUnit?,
        timeout: Long? = null,
        callBack: ShowInterstitialCallback
    ) {
        if (activity == null || activity.isDestroyed || activity.isFinishing || adConfig.admobGuard?.canShowAd(adUnit) == false || adState.isShowingAdFullscreen) {
            callBack.onDismiss()
            return
        }

        if(!interstitialManager.hasAdAvailable()) {
            showLoadingDialog(activity)
        }

        activity.lifecycleScope.launch {
            val isHandled = AtomicBoolean(false)

            var nativeAdJob: Job? = null

            interstitialManager.showInterstitial(
                activity = activity,
                adId = getAdId(adUnit),
                timeoutLong = timeout ?: adConfig.intersTimeout,
                callback = object : ShowInterstitialCallback {
                    override fun onDismiss() {
                        hideLoadingDialog(activity)

                        adState = adState.copy(
                            isIntersShowing = false
                        )
                        nativeAdJob?.cancel()

                    }

                    override fun onShowFailed() {
                        hideLoadingDialog(activity)
                        nativeAdJob?.cancel()

                        callBack.onShowFailed()
                        adState = adState.copy(
                            isIntersShowing = false
                        )
                    }

                    override fun onShow() {
                        hideLoadingDialog(activity)

                        Log.d(TAG, "onShow: Interstitial Dora")

                        adState = adState.copy(
                            isIntersShowing = true
                        )

                        nativeAdJob = CoroutineScope(Dispatchers.Main).launch {
                            nativeManager.adState.first { it.isNotEmpty() }
                            nativeManager.getAndPop(applicationContext ?: return@launch)?.let { nativeAd ->
                                if(isHandled.compareAndSet(false, true)) {
                                    val dialog = NativeFullDialog.newInstance(nativeAd) {
                                        callBack.onDismiss()
                                    }
                                    dialog.show(activity.supportFragmentManager, NativeFullDialog.TAG)
                                }
                            }
                        }

                        if(reloadAdUnit != null) {
                            loadInterstitial(
                                reloadAdUnit
                            )
                        }

                        callBack.onShow()

                    }

                    override fun onImpression() {
                        callBack.onImpression()
                    }
                }
            )
        }
    }

    private fun showLoadingDialog(
        activity: AppCompatActivity
    ) {
        if(activity.supportFragmentManager.isStateSaved) {
            return
        }

        if(activity.supportFragmentManager.findFragmentByTag(LoadingAdDialogFragment.TAG) == null) {
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

    // Native
    fun setNativeAds(
        listAds: List<AdmobUnit>,
        maxAdCache: Int = 2,
        intervalTime: Long = 3000L,
    ) {
        nativeManager.configAd(
            listAds.map {
                getAdId(it)
            },
            maxAdCache,
            intervalTime
        )
    }
    fun loadAndShowNative(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        viewGroup: ViewGroup,
        layout: NativeLayout,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            ensureInitialized()

//            if(adConfig.admobGuard?.canLoadAd(adUnit) == false) {
//                return@launch
//            }

            val layoutAd = when(layout) {
                NativeLayout.Native50 -> R.layout.dora_layout_ads_native_50
                NativeLayout.Native150 -> R.layout.dora_layout_ads_native_150
                NativeLayout.Native250 -> R.layout.dora_layout_ads_native_250
                NativeLayout.NativeFull -> R.layout.dora_layout_ads_native_full
                NativeLayout.NativeFullWithNextButton -> R.layout.dora_layout_ads_native_full_with_next_btn
                NativeLayout.NativeCollapsible -> R.layout.dora_layout_ads_native_collapsible
                is NativeLayout.Custom -> layout.layout
            }

            fun showAd(ad: NativeAd) {
                nativeManager.showNativeAd(
                    activity = activity,
                    nativeAd = ad,
                    viewLifecycle = lifecycleOwner.lifecycle,
                    layoutAd = layoutAd,
                    viewGroup = viewGroup
                )
            }

            val cachedAd = nativeManager.getAndPop(applicationContext?:return@launch)

            if (cachedAd != null) {
                Log.d("Dora", "Show Native from Cache")
                showAd(cachedAd)
                return@launch
            }

            nativeManager.loadAd(applicationContext?:return@launch)

            nativeManager.adState.first { it.isNotEmpty() }

            nativeManager.getAndPop(applicationContext?:return@launch)?.let { ad ->
                nativeManager.showNativeAd(
                    activity = activity,
                    nativeAd = ad,
                    viewLifecycle = lifecycleOwner.lifecycle,
                    layoutAd = layoutAd,
                    viewGroup = viewGroup,
                )
            }
        }
    }
    fun loadNative() {
        CoroutineScope(Dispatchers.IO).launch {
            ensureInitialized()

            nativeManager.loadAd(applicationContext?:return@launch)
        }
    }

    // Banner
    @RequiresPermission(Manifest.permission.INTERNET)
    fun loadBanner(
        activity: Activity,
        container: ViewGroup,
        adSize: AdmobBannerSize,
        lifecycleOwner: LifecycleOwner,
        adUnitId: AdmobUnit
    ) {
        lifecycleOwner.lifecycleScope.launch {
            ensureInitialized()

            val adView = AdView(activity)
            adView.adUnitId = getAdId(adUnitId)

            var extras : Bundle? = null

            when(adSize) {
                AdmobBannerSize.Adaptive -> {
                    adView.setAdSize(getAdaptiveAdSize(activity, container))
                    extras = Bundle()
                    extras.putString("collapsible", "bottom")
                }
                AdmobBannerSize.Banner50 -> {
                    adView.setAdSize(AdSize.BANNER)
                }
            }


            container.removeAllViews()
            container.addView(adView)

            val adRequest = if(extras == null) {
                AdRequest.Builder().build()
            } else {
                AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                    .build()
            }
            adView.loadAd(adRequest)

            lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    adView.resume()
                }

                override fun onPause(owner: LifecycleOwner) {
                    adView.pause()
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    adView.destroy()
                }
            })
        }
    }

    private fun getAdaptiveAdSize(activity: Activity, container: ViewGroup): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        val frameWidth = if (container.width > 0) container.width.toFloat() else displayMetrics.widthPixels.toFloat()
        val adWidth = (frameWidth / displayMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    //OpenAd
    fun registerOpenAd(
        application: Application,
        admobUnit: AdmobUnit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            ensureInitialized()

            openAdManager = OpenAdManager(
                application,
                getAdId(admobUnit),
                object : OpenAdCallback {
                    override fun canShow(): Boolean {
                        return !adState.isShowingAdFullscreen
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

                }
            )
        }
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
                    if(openAdManager?.isAvailable() == true) {
                        isHandled = true
                        openAdManager?.showAdIfAvailable(activity, onDismiss)
                        break
                    }
                    delay(400)
                }
            }

            if(!isHandled) {
                onDismiss()
            }
        }

    }
}