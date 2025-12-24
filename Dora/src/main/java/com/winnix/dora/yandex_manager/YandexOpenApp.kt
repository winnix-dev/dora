package com.winnix.dora.yandex_manager

import android.app.Activity
import android.app.Application
import android.util.Log
import com.winnix.dora.callback.ShowAdCallback
import com.winnix.dora.helper.AdIdProvider
import com.winnix.dora.helper.AdProvider
import com.winnix.dora.model.AdUnit
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData

object YandexOpenApp {
    private var openAd: AppOpenAd? = null
    private var appOpenAdLoader: AppOpenAdLoader? = null
    private var adUnit: AdUnit? = null

    private var adRequestConfiguration: AdRequestConfiguration? = null

    private var isLoading = false

    fun int(application: Application) {
        appOpenAdLoader = AppOpenAdLoader(application)

        val appOpenAdLoadListener = object : AppOpenAdLoadListener {
            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.e("TAGG", "onAdFailedToLoad: Yandex $error", )
                isLoading = false
            }

            override fun onAdLoaded(appOpenAd: AppOpenAd) {

                openAd = appOpenAd
                isLoading = false
            }
        }

        appOpenAdLoader?.setAdLoadListener(appOpenAdLoadListener)
    }

    fun setAdUnit(adUnit: AdUnit) {
        this.adUnit = adUnit

        adRequestConfiguration = AdRequestConfiguration.Builder(
            AdIdProvider.getAdId(
                adUnit,
                AdProvider.YANDEX
            )
        ).build()

        loadAd()
    }

    fun isAdAvailable() = openAd != null

    fun loadAd() {
        if (openAd != null || isLoading || adRequestConfiguration == null || appOpenAdLoader == null) return

        isLoading = true
        appOpenAdLoader?.loadAd(adRequestConfiguration!!)
    }

    fun showAd(activity: Activity, callback: ShowAdCallback) {
        if (openAd == null) {
            callback.onShowFail()
            return
        }

        val appOpenListener = object : AppOpenAdEventListener {
            override fun onAdClicked() {

            }

            override fun onAdDismissed() {
                callback.onDismiss()

                openAd = null
                loadAd()
            }

            override fun onAdFailedToShow(adError: AdError) {
                Log.e("Dora", "Yandex OpenApp Show Fail: $adError")
                callback.onShowFail()

                openAd = null
                loadAd()
            }

            override fun onAdImpression(impressionData: ImpressionData?) { }

            override fun onAdShown() {
                callback.onShowSuccess()
            }

        }

        openAd?.setAdEventListener(appOpenListener)
        openAd?.show(activity)
    }

}