package com.winnix.dora.admob_manager

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.winnix.dora.Dora
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.helper.DoraLogger
import com.winnix.dora.model.AdType
import com.winnix.dora.model.DoraAdError
import com.winnix.dora.model.InterstitialResult
import com.winnix.dora.model.InterstitialState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object AdmobInterstitial {
    private val _interstitialAd = MutableStateFlow<InterstitialState>(InterstitialState.Idle)
    val interstitialAd = _interstitialAd.asStateFlow()

    fun loadAd(
        context: Context,
        id: String,
        listener: LoadInterstitialCallback? = null
    ) {
        val state = _interstitialAd.value

        if (state is InterstitialState.Success || state is InterstitialState.Loading) return
        _interstitialAd.update { InterstitialState.Loading }

        CoroutineScope(Dispatchers.Main).launch {
            Dora.ensureInitialized()

            val adRequest = AdRequest.Builder().build()
            listener?.onBeginLoad()
            InterstitialAd.load(
                context.applicationContext,
                id,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(p0: InterstitialAd) {
                        DoraLogger.logAdMobLoadSuccess(AdType.Inters, id)
                        _interstitialAd.update {
                            InterstitialState.Success(
                                InterstitialResult.AdMob(
                                    p0
                                )
                            )
                        }
                        listener?.onLoaded()
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        DoraLogger.logAdMobLoadFail(AdType.Inters, id, p0)
                        _interstitialAd.update { InterstitialState.Failed }

                        listener?.onFailed(
                            DoraAdError(
                                errorMessage = p0.message,
                                errorCode = p0.code
                            )
                        )
                    }
                }
            )
        }
    }

    fun showAd(
        activity: Activity,
        listener: ShowInterstitialCallback
    ) {
        val state = _interstitialAd.value
        if (state is InterstitialState.Success && state.data is InterstitialResult.AdMob) {
            state.data.ad.apply {
                fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        listener.onDismiss()
                    }

                    override fun onAdShowedFullScreenContent() {
                        _interstitialAd.update { InterstitialState.Idle }

                        listener.onShow()
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        DoraLogger.logAdMobShowFail( AdType.Inters, p0)
                        _interstitialAd.update { InterstitialState.Failed }

                        listener.onShowFailed(
                            DoraAdError(
                                errorCode = p0.code,
                                errorMessage = p0.message
                            )
                        )
                    }
                }

                show(activity)
            }
        } else {
            listener.onShowFailed(
                DoraAdError(
                    errorCode = 1924,
                    errorMessage = "Ad not Available"
                )
            )
        }
    }
}