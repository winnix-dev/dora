package com.winnix.dora.yandex_manager

import android.app.Activity
import android.content.Context
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.helper.DoraLogger
import com.winnix.dora.model.AdType
import com.winnix.dora.model.DoraAdError
import com.winnix.dora.model.InterstitialResult
import com.winnix.dora.model.InterstitialState
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
    private val _interstitialAd = MutableStateFlow<InterstitialState>(InterstitialState.Idle)
    val interstitialAd = _interstitialAd.asStateFlow()

    fun loadAd(
        context: Context,
        id: String,
        listener: LoadInterstitialCallback? = null
    ) {
        val state = _interstitialAd.value
        if (state is InterstitialState.Loading || state is InterstitialState.Success) {
            return
        }

        listener?.onBeginLoad()

        val interstitialAdLoader = InterstitialAdLoader(context.applicationContext).apply {
            setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    DoraLogger.logYandexLoadSuccess(AdType.Inters,id)
                    _interstitialAd.update { InterstitialState.Success(InterstitialResult.Yandex(interstitialAd)) }

                    listener?.onLoaded()
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    DoraLogger.logYandexLoadFail(AdType.Inters,id, error)
                    _interstitialAd.update { InterstitialState.Failed }

                    listener?.onFailed(
                        DoraAdError(
                            errorCode = error.code,
                            errorMessage = error.description
                        )
                    )
                }
            })
        }

        val adRequest = AdRequestConfiguration.Builder(id).build()

        _interstitialAd.update { InterstitialState.Loading }
        interstitialAdLoader.loadAd(adRequest)
    }

    fun showAd(
        activity: Activity,
        listener: ShowInterstitialCallback
    ) {
        val state = _interstitialAd.value

        if (state is InterstitialState.Success && state.data is InterstitialResult.Yandex) {
            state.data.ad.apply {
                setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() {
                        _interstitialAd.update { InterstitialState.Idle }
                        listener.onShow()
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        DoraLogger.logYandexShowFail(AdType.Inters, adError)
                        _interstitialAd.update { InterstitialState.Idle }
                        listener.onShowFailed(
                            DoraAdError(
                                errorCode = 0,
                                errorMessage = adError.description
                            )
                        )
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
            listener.onShowFailed(
                DoraAdError(
                    errorCode = 1924,
                    errorMessage = "No Ad Available"
                )
            )
        }
    }

}