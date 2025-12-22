package com.winnix.dora.yandex_manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.winnix.dora.Dora
import com.winnix.dora.callback.ShowInterstitialCallback
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
import kotlinx.coroutines.launch

internal object YandexIntersManager {
    private var interstitialAd: InterstitialAd? = null
    private var isIntersLoading = false
    private var isWaiting = false
    private var adUnit : AdUnit? = null

    fun loadInterstitialAd(
        context: Context,
    ) {
        if(interstitialAd != null || isIntersLoading || adUnit == null || isWaiting) {
            return
        }

        val interstitialAdLoader = InterstitialAdLoader(context.applicationContext).apply {
            setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    isIntersLoading = false
                    this@YandexIntersManager.interstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e("Dora", "Load Ad Failed $error")
                    isIntersLoading = false
                    interstitialAd = null

                    isWaiting = true

                    CoroutineScope(Dispatchers.Main).launch {
                        delay(6000)
                        isWaiting = false
                        loadInterstitialAd(context)
                    }
                }
            })
        }

        val adRequest = AdRequestConfiguration.Builder(Dora.getYandexId(adUnit!!)).build()
        
        isIntersLoading = true
        interstitialAdLoader.loadAd(adRequest)
    }

    fun showInterstitial(
        activity: Activity,
        callback: ShowInterstitialCallback,
    ) {
        if(interstitialAd != null) {
            interstitialAd?.apply {
                setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() {
                        callback.onShow()
                    }
                    override fun onAdFailedToShow(adError: AdError) {
                        interstitialAd?.setAdEventListener(null)
                        interstitialAd = null

                        Log.e("Dora", "yandex show failed: $adError", )

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
}