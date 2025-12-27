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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.winnix.dora.admob_manager.AdmobBannerHelper
import com.winnix.dora.admob_manager.AdmobOpenAppManager
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.OpenAdCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.admob_manager.InterstitialLegacyManager
import com.winnix.dora.admob_manager.AdmobInterstitialManager
import com.winnix.dora.admob_manager.NativeLayout
import com.winnix.dora.admob_manager.AdmobNativeManager
import com.winnix.dora.callback.LoadBannerCallback
import com.winnix.dora.helper.OpenAdManager
import com.winnix.dora.helper.AdIdProvider
import com.winnix.dora.helper.AdProvider
import com.winnix.dora.helper.LoadAdEnum
import com.winnix.dora.helper.UMPHelper
import com.winnix.dora.model.AdConfig
import com.winnix.dora.model.AdState
import com.winnix.dora.model.AdmobBannerSize
import com.winnix.dora.model.AdUnit
import com.winnix.dora.rule.AdmobGuard
import com.winnix.dora.ui.LoadingAdDialogFragment
import com.winnix.dora.ui.NativeFullDialog
import com.winnix.dora.yandex_manager.YandexBannerManager
import com.winnix.dora.yandex_manager.YandexIntersManager
import com.winnix.dora.yandex_manager.YandexNativeLayout
import com.winnix.dora.yandex_manager.YandexNativeManger
import com.winnix.dora.yandex_manager.YandexOpenApp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    private var interstitialLegacyManager = InterstitialLegacyManager()

    private var openAdManager: OpenAdManager? = null

    private var adState = AdState()

    internal var adProvider: Map<String, String> = mapOf()

    private var adGuard = AdmobGuard()

    // General
    fun initialize(
        activity: Activity,
        adConfig: AdConfig
    ) {
        Log.d(TAG, "initAd SDK")

        this.adConfig = adConfig

        AdIdProvider.isDebug = adConfig.isDebug

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

    fun setAdProvider(
        adProvider: Map<String, String>
    ) {
        this.adProvider = adProvider
    }

    fun setAdGuard(admobGuard: AdmobGuard) {
        this.adGuard = admobGuard
    }

    fun setUpAdmob(
        intersList: List<AdUnit>,
        nativeList: List<AdUnit>,
        openAppId: AdUnit,
        maxNativeCache: Int = 2,
        nativeIntervalTime: Long = 3000L,
        loadIntersCallback: LoadInterstitialCallback? = null
    ) {
        applicationContext?.let { context ->
            AdmobInterstitialManager.setUp(
                listAd = intersList,
                callback = loadIntersCallback,
                context = context
            )
            AdmobNativeManager.configAd(
                nativeList,
                maxNativeCache,
                nativeIntervalTime
            )
            AdmobNativeManager.loadAd(context)

            AdmobOpenAppManager.adUnit = openAppId
        }
    }

    fun setUpYandex(
        intersUnit: AdUnit,
        nativeUnit: AdUnit,
        bannerUnit: AdUnit,
        openAppId: AdUnit
    ) {
        applicationContext?.let {
            YandexIntersManager.setUpInters(intersUnit, it)
            YandexNativeManger.setUp(nativeUnit, it)
            YandexBannerManager.setUp(bannerUnit)
            YandexOpenApp.setAdUnit(openAppId)
        }
    }


    // Inters
    fun loadInterstitialLegacy(
        adUnit: AdUnit,
    ) {
        if (applicationContext == null || !adGuard.checkAd()) return

        CoroutineScope(Dispatchers.Main).launch {
            ensureInitialized()

            interstitialLegacyManager.loadInterstitial(
                applicationContext ?: return@launch,
                AdIdProvider.getAdId(adUnit, AdProvider.AD_MOB),
            )
        }
    }

    fun showInterstitialLegacy(
        activity: AppCompatActivity?,
        adUnit: AdUnit,
        reloadAdUnit: AdUnit?,
        timeout: Long? = null,
        callBack: ShowInterstitialCallback
    ) {
        if (activity == null || activity.isDestroyed || activity.isFinishing || adState.isShowingAdFullscreen || !adGuard.checkInters()) {
            callBack.onDismiss()
            return
        }

        if (!interstitialLegacyManager.hasAdAvailable()) {
            showLoadingDialog(activity)
        }

        activity.lifecycleScope.launch {
            val isHandled = AtomicBoolean(false)

            var nativeAdJob: Job? = null

            interstitialLegacyManager.showInterstitial(
                activity = activity,
                adId = AdIdProvider.getAdId(adUnit, AdProvider.AD_MOB),
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
                            AdmobNativeManager.adState.first { it.isNotEmpty() }
                            AdmobNativeManager.getAndPop(applicationContext ?: return@launch)
                                ?.let { nativeAd ->

                                    if (canShowDialog(activity, NativeFullDialog.TAG)) {
                                        if (isHandled.compareAndSet(false, true)) {
                                            val dialog = NativeFullDialog.newInstance(nativeAd) {
                                                callBack.onDismiss()
                                                adState = adState.copy(
                                                    isNativeFullShowing = false
                                                )
                                            }
                                            adState = adState.copy(
                                                isNativeFullShowing = true
                                            )
                                            dialog.show(
                                                activity.supportFragmentManager,
                                                NativeFullDialog.TAG
                                            )
                                        }
                                    }
                                }
                        }

                        if (reloadAdUnit != null) {
                            loadInterstitialLegacy(
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

    fun showInterstitial(
        activity: AppCompatActivity?,
        timeout: Long? = null,
        callback: ShowInterstitialCallback
    ) {
        if (activity == null || activity.isDestroyed || activity.isFinishing || adState.isShowingAdFullscreen || !adGuard.checkInters()) {
            callback.onDismiss()
            return
        }

        AdmobInterstitialManager.loadAd(activity)
        YandexIntersManager.loadInterstitialAd(activity)

        if (!AdmobInterstitialManager.isAvailable()) {
            showLoadingDialog(activity)
        }

        activity.lifecycleScope.launch {
            val result = withTimeoutOrNull(timeout ?: adConfig.intersTimeout) {
                AdmobInterstitialManager.adState.first {
                    it != null
                }
            }

            var nativeAdJob: Job? = null
            val isHandled = AtomicBoolean(false)

            if (result != null) {
                result.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        AdmobInterstitialManager.onConsumed(activity)

                        hideLoadingDialog(activity)

                        Log.d(TAG, "onShow: Interstitial Dora")

                        adState = adState.copy(
                            isIntersShowing = true
                        )

                        if(adGuard.checkNativeFull()) {
                            nativeAdJob = CoroutineScope(Dispatchers.Main).launch {
                                AdmobNativeManager.adState.first { it.isNotEmpty() }
                                AdmobNativeManager.getAndPop(applicationContext ?: return@launch)
                                    ?.let { nativeAd ->
                                        if (canShowDialog(activity, NativeFullDialog.TAG)) {
                                            if (isHandled.compareAndSet(false, true)) {
                                                val dialog = NativeFullDialog.newInstance(nativeAd) {
                                                    callback.onDismiss()
                                                    adState = adState.copy(
                                                        isNativeFullShowing = false
                                                    )
                                                }
                                                adState = adState.copy(
                                                    isNativeFullShowing = true
                                                )
                                                dialog.show(
                                                    activity.supportFragmentManager,
                                                    NativeFullDialog.TAG
                                                )
                                            }
                                        }
                                    }
                            }
                        }

                        callback.onShow()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        hideLoadingDialog(activity)

                        adState = adState.copy(
                            isIntersShowing = false
                        )
                        nativeAdJob?.cancel()

                        if (isHandled.compareAndSet(false, true)) {
                            callback.onDismiss()
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        AdmobInterstitialManager.onConsumed(activity)
                        hideLoadingDialog(activity)
                        if (YandexIntersManager.isAvailable()) {
                            YandexIntersManager.showInterstitial(
                                activity,
                                callback = object : ShowInterstitialCallback {
                                    override fun onDismiss() {
                                        adState = adState.copy(
                                            isIntersShowing = false
                                        )
                                        nativeAdJob?.cancel()

                                        if (isHandled.compareAndSet(false, true)) {
                                            callback.onDismiss()
                                        }
                                    }

                                    override fun onShowFailed() {
                                        callback.onDismiss()

                                        nativeAdJob?.cancel()

                                        callback.onShowFailed()

                                        adState = adState.copy(
                                            isIntersShowing = false
                                        )
                                    }

                                    override fun onShow() {
                                        if(adGuard.checkNativeFull()) {
                                            nativeAdJob = CoroutineScope(Dispatchers.Main).launch {
                                                AdmobNativeManager.adState.first { it.isNotEmpty() }
                                                AdmobNativeManager.getAndPop(
                                                    applicationContext ?: return@launch
                                                )?.let { nativeAd ->
                                                    if (canShowDialog(activity, NativeFullDialog.TAG)) {
                                                        if (isHandled.compareAndSet(false, true)) {
                                                            val dialog =
                                                                NativeFullDialog.newInstance(nativeAd) {
                                                                    callback.onDismiss()
                                                                    adState = adState.copy(
                                                                        isNativeFullShowing = false
                                                                    )
                                                                }
                                                            adState = adState.copy(
                                                                isNativeFullShowing = true
                                                            )
                                                            dialog.show(
                                                                activity.supportFragmentManager,
                                                                NativeFullDialog.TAG
                                                            )
                                                        }
                                                    }

                                                }
                                            }
                                        }

                                        adState = adState.copy(
                                            isIntersShowing = true
                                        )

                                        callback.onShow()
                                    }
                                }
                            )
                        }
                        else {
                            callback.onShowFailed()
                        }
                    }

                    override fun onAdImpression() {
                        callback.onImpression()
                    }
                }

                result.show(activity)
            } else {
                hideLoadingDialog(activity)

                YandexIntersManager.showInterstitial(
                    activity,
                    callback = object : ShowInterstitialCallback {
                        override fun onDismiss() {
                            adState = adState.copy(
                                isIntersShowing = false
                            )
                            nativeAdJob?.cancel()

                            if (isHandled.compareAndSet(false, true)) {
                                callback.onDismiss()
                            }
                        }

                        override fun onShowFailed() {
                            callback.onDismiss()

                            nativeAdJob?.cancel()

                            callback.onShowFailed()

                            adState = adState.copy(
                                isIntersShowing = false
                            )
                        }

                        override fun onShow() {
                            adState = adState.copy(
                                isIntersShowing = true
                            )
                            callback.onShow()
                            if (adGuard.checkNativeFull()) {
                                nativeAdJob = CoroutineScope(Dispatchers.Main).launch {
                                    AdmobNativeManager.adState.first { it.isNotEmpty() }
                                    AdmobNativeManager.getAndPop(
                                        applicationContext ?: return@launch
                                    )?.let { nativeAd ->
                                        if (canShowDialog(activity, NativeFullDialog.TAG)) {
                                            if (isHandled.compareAndSet(false, true)) {
                                                val dialog = NativeFullDialog.newInstance(nativeAd) {
                                                    callback.onDismiss()

                                                    adState = adState.copy(
                                                        isNativeFullShowing = false
                                                    )
                                                }
                                                adState = adState.copy(
                                                    isNativeFullShowing = true
                                                )
                                                dialog.show(
                                                    activity.supportFragmentManager,
                                                    NativeFullDialog.TAG
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )

            }
        }
    }

    fun showInterstitialInNoTime(
        activity: AppCompatActivity?,
        callback: ShowInterstitialCallback
    ) {
        if (activity == null || activity.isDestroyed || activity.isFinishing || adState.isShowingAdFullscreen || !adGuard.checkInters()) {
            callback.onDismiss()
            return
        }

        activity.lifecycleScope.launch {

            var nativeAdJob: Job? = null
            val isHandled = AtomicBoolean(false)

            if (AdmobInterstitialManager.adState.value != null) {

                AdmobInterstitialManager.adState.value?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            AdmobInterstitialManager.onConsumed(activity)

                            adState = adState.copy(
                                isIntersShowing = true
                            )

                            if (adGuard.checkNativeFull()) {
                                nativeAdJob = CoroutineScope(Dispatchers.Main).launch {
                                    AdmobNativeManager.adState.first { it.isNotEmpty() }
                                    AdmobNativeManager.getAndPop(
                                        applicationContext ?: return@launch
                                    )
                                        ?.let { nativeAd ->
                                            if (canShowDialog(activity, NativeFullDialog.TAG)) {
                                                if (isHandled.compareAndSet(false, true)) {
                                                    val dialog =
                                                        NativeFullDialog.newInstance(nativeAd) {
                                                            callback.onDismiss()
                                                            adState = adState.copy(
                                                                isNativeFullShowing = false
                                                            )
                                                        }
                                                    adState = adState.copy(
                                                        isNativeFullShowing = true
                                                    )
                                                    dialog.show(
                                                        activity.supportFragmentManager,
                                                        NativeFullDialog.TAG
                                                    )
                                                }
                                            }
                                        }
                                }
                            }

                            callback.onShow()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            hideLoadingDialog(activity)

                            adState = adState.copy(
                                isIntersShowing = false
                            )
                            nativeAdJob?.cancel()

                            if (isHandled.compareAndSet(false, true)) {
                                callback.onDismiss()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                            AdmobInterstitialManager.onConsumed(activity)
                            hideLoadingDialog(activity)
                            if (YandexIntersManager.isAvailable()) {
                                YandexIntersManager.showInterstitial(
                                    activity,
                                    callback = object : ShowInterstitialCallback {
                                        override fun onDismiss() {
                                            adState = adState.copy(
                                                isIntersShowing = false
                                            )
                                            nativeAdJob?.cancel()

                                            if (isHandled.compareAndSet(false, true)) {
                                                callback.onDismiss()
                                            }
                                        }

                                        override fun onShowFailed() {
                                            callback.onDismiss()

                                            nativeAdJob?.cancel()

                                            callback.onShowFailed()
                                            adState = adState.copy(
                                                isIntersShowing = false
                                            )
                                        }

                                        override fun onShow() {
                                            callback.onShow()
                                            if (adGuard.checkNativeFull()) {
                                                nativeAdJob =
                                                    CoroutineScope(Dispatchers.Main).launch {
                                                        AdmobNativeManager.adState.first { it.isNotEmpty() }
                                                        AdmobNativeManager.getAndPop(
                                                            applicationContext ?: return@launch
                                                        )?.let { nativeAd ->
                                                            if (canShowDialog(
                                                                    activity,
                                                                    NativeFullDialog.TAG
                                                                )
                                                            ) {
                                                                if (isHandled.compareAndSet(
                                                                        false,
                                                                        true
                                                                    )
                                                                ) {
                                                                    val dialog =
                                                                        NativeFullDialog.newInstance(
                                                                            nativeAd
                                                                        ) {
                                                                            callback.onDismiss()
                                                                            adState = adState.copy(
                                                                                isNativeFullShowing = false
                                                                            )
                                                                        }
                                                                    adState = adState.copy(
                                                                        isNativeFullShowing = true
                                                                    )
                                                                    dialog.show(
                                                                        activity.supportFragmentManager,
                                                                        NativeFullDialog.TAG
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                            }

                                            adState = adState.copy(
                                                isIntersShowing = true
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        override fun onAdImpression() {
                            callback.onImpression()
                        }
                    }

                AdmobInterstitialManager.adState.value?.show(activity)
            } else if (YandexIntersManager.isAvailable()) {
                YandexIntersManager.showInterstitial(
                    activity,
                    callback = object : ShowInterstitialCallback {
                        override fun onDismiss() {
                            adState = adState.copy(
                                isIntersShowing = false
                            )
                            nativeAdJob?.cancel()

                            if (isHandled.compareAndSet(false, true)) {
                                callback.onDismiss()
                            }
                        }

                        override fun onShowFailed() {
                            callback.onDismiss()

                            nativeAdJob?.cancel()

                            callback.onShowFailed()

                            adState = adState.copy(
                                isIntersShowing = false
                            )
                        }

                        override fun onShow() {
                            adState = adState.copy(
                                isIntersShowing = true
                            )
                            if (adGuard.checkNativeFull()) {
                                nativeAdJob = CoroutineScope(Dispatchers.Main).launch {
                                    AdmobNativeManager.adState.first { it.isNotEmpty() }
                                    AdmobNativeManager.getAndPop(
                                        applicationContext ?: return@launch
                                    )?.let { nativeAd ->
                                        if (canShowDialog(activity, NativeFullDialog.TAG)) {
                                            if (isHandled.compareAndSet(false, true)) {
                                                val dialog =
                                                    NativeFullDialog.newInstance(nativeAd) {
                                                        callback.onDismiss()

                                                        adState = adState.copy(
                                                            isNativeFullShowing = false
                                                        )
                                                    }
                                                adState = adState.copy(
                                                    isNativeFullShowing = true
                                                )
                                                dialog.show(
                                                    activity.supportFragmentManager,
                                                    NativeFullDialog.TAG
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            callback.onShow()
                        }
                    }
                )
            } else {
                callback.onDismiss()
            }
        }
    }

    suspend fun waitForInterstitialAdmobAndYandex(
        timeout: Long
    ): Boolean {
        if (!adGuard.checkInters()) {
            return false
        }

        val result: Boolean? = withTimeoutOrNull(timeout) {
            var result = false
            getInterLoadState()
                .first { it == LoadAdEnum.SUCCESS || it == LoadAdEnum.FAILED }

            if (getIntersState().value != null) {
                result = true
            } else {
                getYandexState()
                    .first { it == LoadAdEnum.SUCCESS || it == LoadAdEnum.FAILED }

                if (isYandexInterstitialAvailable()) {
                    result = true
                }
            }

            result
        }
        return result == true
    }

    fun getIntersState() = AdmobInterstitialManager.adState
    fun getInterLoadState() = AdmobInterstitialManager.loadState
    fun isYandexInterstitialAvailable(): Boolean = YandexIntersManager.isAvailable()

    fun showYandexInterstitialIfAvailable(
        activity: Activity,
        callback: ShowInterstitialCallback,
    ) {
        if (!adGuard.checkInters()) {
            callback.onDismiss()
            return
        }

        YandexIntersManager.showInterstitial(activity, callback)
    }

    fun getYandexState() = YandexIntersManager.adState

    // Native
    fun loadAndShowNative(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        viewGroup: ViewGroup,
        layout: NativeLayout,
        yandexNativeLayout: YandexNativeLayout? = null
    ) {
        if (!adGuard.checkNative()) {
            return
        }
        lifecycleOwner.lifecycleScope.launch {
            ensureInitialized()

            val layoutAd = when (layout) {
                NativeLayout.Native50 -> R.layout.dora_layout_ads_native_50
                NativeLayout.Native150 -> R.layout.dora_layout_ads_native_150
                NativeLayout.Native250 -> R.layout.dora_layout_ads_native_250
                NativeLayout.NativeFull -> R.layout.dora_layout_ads_native_full
                NativeLayout.NativeFullWithNextButton -> R.layout.dora_layout_ads_native_full_with_next_btn
                NativeLayout.NativeCollapsible -> R.layout.dora_layout_ads_native_collapsible
                is NativeLayout.Custom -> layout.layout
            }

            fun showAdMob(ad: NativeAd) {
                AdmobNativeManager.showNativeAd(
                    activity = activity,
                    nativeAd = ad,
                    viewLifecycle = lifecycleOwner.lifecycle,
                    layoutAd = layoutAd,
                    viewGroup = viewGroup
                )
            }

            val cachedAd = AdmobNativeManager.getAndPop(applicationContext ?: return@launch)

            if (cachedAd != null) {
                Log.d("Dora", "Show Native from Cache")
                showAdMob(cachedAd)
                return@launch
            }

            AdmobNativeManager.loadAd(applicationContext ?: return@launch)

            val isAnyAdAvailable: Flow<Boolean> = combine(
                AdmobNativeManager.adState,
                YandexNativeManger.nativeAd
            ) { admobNatives, yandexNative ->

                admobNatives.isNotEmpty() || yandexNative != null
            }

            isAnyAdAvailable.first { it }

            val admob = AdmobNativeManager.getAndPop(activity)

            if (admob != null) {
                AdmobNativeManager.showNativeAd(
                    activity = activity,
                    nativeAd = admob,
                    viewLifecycle = lifecycleOwner.lifecycle,
                    layoutAd = layoutAd,
                    viewGroup = viewGroup,
                )
            } else {
                val nativeYandex = YandexNativeManger.nativeAd.value
                if (nativeYandex != null) {
                    YandexNativeManger.showNativeAd(
                        viewGroup = viewGroup,
                        inflater = activity.layoutInflater,
                        yandexNativeLayout
                    )
                }
            }
        }
    }

    fun loadNative() {
        if (!adGuard.checkAd()) return
        CoroutineScope(Dispatchers.IO).launch {
            ensureInitialized()

            AdmobNativeManager.loadAd(applicationContext ?: return@launch)
        }
    }

    // Banner
    @RequiresPermission(Manifest.permission.INTERNET)
    fun loadBanner(
        activity: Activity,
        container: ViewGroup,
        adSize: AdmobBannerSize,
        lifecycleOwner: LifecycleOwner,
        adUnitId: AdUnit
    ) {
        if (!adGuard.checkBanner()) return

        lifecycleOwner.lifecycleScope.launch {
            ensureInitialized()

            AdmobBannerHelper.loadBanner(
                id = AdIdProvider.getAdId(adUnitId, AdProvider.AD_MOB),
                activity = activity,
                container = container,
                adSize = adSize,
                lifecycleOwner = lifecycleOwner,
                callback = object : LoadBannerCallback {
                    override fun onLoadFailed() {
                        YandexBannerManager.loadBanner(
                            activity = activity,
                            container = container,
                            lifecycleOwner = lifecycleOwner,
                            callback = object : LoadBannerCallback {},
                        )
                    }
                }
            )
        }
    }

    //OpenAd
    fun registerOpenAd(
        application: Application,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            ensureInitialized()

            openAdManager = OpenAdManager(
                application,
                object : OpenAdCallback {
                    override fun canShow(): () -> Boolean {
                        return {
                            !adState.isShowingAdFullscreen && adGuard.checkOpenApp()
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
                        return { adGuard.checkOpenApp() }
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
        if (!adGuard.checkOpenApp()) {
            onDismiss()
            return
        }
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

}