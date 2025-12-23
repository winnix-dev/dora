package com.winnix.dora.yandex_manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.winnix.dora.Dora
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.helper.AdIdProvider
import com.winnix.dora.helper.AdProvider
import com.winnix.dora.helper.LoadAdEnum
import com.winnix.dora.model.AdUnit
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object YandexIntersManager {
    private var interstitialAd: InterstitialAd? = null
    private var isIntersLoading = false
    private var isWaiting = false
    private var adUnit: AdUnit? = null

    private val _adState = MutableStateFlow(LoadAdEnum.IDLE)
    val adState = _adState.asStateFlow()

    fun setUpInters(adUnit: AdUnit, context: Context) {
        this.adUnit = adUnit

        loadInterstitialAd(context)
    }

    fun loadInterstitialAd(
        context: Context,
    ) {
        if (interstitialAd != null || isIntersLoading || adUnit == null || isWaiting) {
            return
        }

        val interstitialAdLoader = InterstitialAdLoader(context.applicationContext).apply {
            setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    isIntersLoading = false
                    _adState.update { LoadAdEnum.SUCCESS }
                    this@YandexIntersManager.interstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e("Dora", "Load Ad Failed $error")
                    isIntersLoading = false
                    interstitialAd = null

                    isWaiting = true
                    _adState.update { LoadAdEnum.FAILED }
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(6000)
                        isWaiting = false
                        loadInterstitialAd(context)
                    }
                }
            })
        }

        val adRequest =
            AdRequestConfiguration.Builder(
                AdIdProvider.getAdId(adUnit!!, AdProvider.YANDEX)
            ).build()

        _adState.update { LoadAdEnum.LOADING }
        isIntersLoading = true
        interstitialAdLoader.loadAd(adRequest)
    }

    fun showInterstitial(
        activity: Activity,
        callback: ShowInterstitialCallback,
    ) {
        if (interstitialAd != null) {
            interstitialAd?.apply {
                setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() {
                        callback.onShow()
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        interstitialAd?.setAdEventListener(null)
                        interstitialAd = null

                        Log.e("Dora", "yandex show failed: $adError")

                        loadInterstitialAd(activity)
                        callback.onShowFailed()
                    }

                    override fun onAdDismissed() {
                        interstitialAd?.setAdEventListener(null)
                        interstitialAd = null

                        loadInterstitialAd(activity)
                        callback.onDismiss()
                    }

                    override fun onAdClicked() {}

                    override fun onAdImpression(impressionData: ImpressionData?) {}
                })
                show(activity)
            }
        } else {
            callback.onShowFailed()
        }
    }

    fun isAvailable(): Boolean = interstitialAd != null
}