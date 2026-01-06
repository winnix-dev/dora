package com.winnix.dora.admob_manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.winnix.dora.Dora
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.helper.AdProvider
import com.winnix.dora.model.InterstitialResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object AdmobInterstitial {
    private val _interstitialAd = MutableStateFlow<InterstitialResult>(InterstitialResult.Idle)
    val interstitialAd = _interstitialAd.asStateFlow()

    fun loadAd(
        context: Context,
        id: String,
        listener: LoadInterstitialCallback? = null
    ) {
        val state = _interstitialAd.value

        if (state is InterstitialResult.Success || state is InterstitialResult.Loading) return
        _interstitialAd.update { InterstitialResult.Loading }

        CoroutineScope(Dispatchers.Main).launch {
            Dora.ensureInitialized()

            val adRequest = AdRequest.Builder().build()
            listener?.onBeginLoad(adProvider = AdProvider.AD_MOB,)
            InterstitialAd.load(
                context.applicationContext,
                id,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(p0: InterstitialAd) {

                        _interstitialAd.update { InterstitialResult.Success(p0) }

                        listener?.onLoaded(AdProvider.AD_MOB)
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        _interstitialAd.update { InterstitialResult.Failed }

                        listener?.onFailed(
                            adProvider = AdProvider.AD_MOB,
                            errorCode = p0.code,
                            errorMessage = p0.message
                        )
                    }

                })
        }
    }

    fun showAd(
        activity: Activity,
        listener: ShowInterstitialCallback
    ) {
        val state = _interstitialAd.value
        if(state is InterstitialResult.Success) {
            state.ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    listener.onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    _interstitialAd.update { InterstitialResult.Idle }

                    listener.onShow()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    Log.e("Dora", p0.toString())
                    _interstitialAd.update { InterstitialResult.Failed }

                    listener.onShowFailed()
                }
            }

            state.ad.show(activity)
        }
        else {
            listener.onShowFailed()
        }

    }

}