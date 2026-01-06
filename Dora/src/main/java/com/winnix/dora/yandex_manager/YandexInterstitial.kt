package com.winnix.dora.yandex_manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.helper.AdProvider
import com.winnix.dora.model.YandexInterstitialResult
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal object YandexInterstitial {
    private val _interstitialAd = MutableStateFlow<YandexInterstitialResult>(YandexInterstitialResult.Idle)
    val interstitialAd = _interstitialAd.asStateFlow()

    fun loadAd(
        context: Context,
        id: String,
        listener: LoadInterstitialCallback? = null
    ) {
        val state = _interstitialAd.value
        if (state is YandexInterstitialResult.Loading || state is YandexInterstitialResult.Success) {
            return
        }

        listener?.onBeginLoad(adProvider = AdProvider.YANDEX,)

        val interstitialAdLoader = InterstitialAdLoader(context.applicationContext).apply {
            setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    _interstitialAd.update { YandexInterstitialResult.Success(interstitialAd) }

                    listener?.onLoaded(adProvider = AdProvider.YANDEX,)
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e("Dora", "Load Inters Yandex Failed $error")
                    _interstitialAd.update { YandexInterstitialResult.Failed }

                    listener?.onFailed(
                        adProvider = AdProvider.YANDEX,
                        errorCode = error.code,
                        errorMessage = error.description
                    )
                }
            })
        }

        val adRequest = AdRequestConfiguration.Builder(id).build()

        _interstitialAd.update { YandexInterstitialResult.Loading }
        interstitialAdLoader.loadAd(adRequest)
    }

    fun showAd(
        activity: Activity,
        listener: ShowInterstitialCallback
    ) {
        val state = _interstitialAd.value

        if (state is YandexInterstitialResult.Success) {
            state.ad.apply {
                setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() {
                        _interstitialAd.update { YandexInterstitialResult.Idle }
                        listener.onShow()
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        Log.e("Dora", "onAdFailedToShow: $adError")
                        _interstitialAd.update { YandexInterstitialResult.Idle }
                        listener.onShowFailed()
                    }

                    override fun onAdDismissed() {
                        listener.onDismiss()
                    }

                    override fun onAdClicked() {}

                    override fun onAdImpression(impressionData: ImpressionData?) {
                        listener.onImpression()
                    }
                })
                show(activity)
            }
        }
        else {
            listener.onShowFailed()
        }
    }

}