package com.winnix.dora.helper

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class InterstitialLegacyManager {
    private val channel = Channel<Boolean>(Channel.CONFLATED)
    private var isLoading = false
    private var mInterstitialAd: InterstitialAd? = null

    fun loadInterstitial(
        context: Context,
        id: String,
        callback: LoadInterstitialCallback? = null
    ) {
        val appContext = context.applicationContext

        if (mInterstitialAd != null || isLoading) {
            return
        }

        isLoading = true
        channel.tryReceive()

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            appContext,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: InterstitialAd) {
                    callback?.onLoaded()

                    mInterstitialAd = p0
                    isLoading = false

                    CoroutineScope(Dispatchers.Default).launch {
                        channel.send(true)
                    }
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    callback?.onFailed(p0)

                    mInterstitialAd = null
                    isLoading = false

                    CoroutineScope(Dispatchers.Default).launch {
                        channel.send(false)
                    }
                }
            }
        )

    }

    suspend fun showInterstitial(
        activity: Activity,
        adId: String,
        timeoutLong: Long,
        callback: ShowInterstitialCallback
    ) {
        if ( mInterstitialAd == null) {
            if(!isLoading) {
                loadInterstitial(
                    activity,
                    adId,
                    object : LoadInterstitialCallback {}
                )
            }

            withTimeoutOrNull(timeoutLong) {
                channel.receive()
            }
        }

        if(mInterstitialAd != null) {
            showInterstitialInternal(
                activity,
                callback
            )
        } else {
            callback.onShowFailed()
        }

    }

    private fun showInterstitialInternal(
        activity: Activity,
        callback: ShowInterstitialCallback
    ) {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                mInterstitialAd = null

                callback.onShow()
            }

            override fun onAdDismissedFullScreenContent() {
                callback.onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                mInterstitialAd = null

                callback.onShowFailed()
            }

            override fun onAdImpression() {
                callback.onImpression()
            }
        }

        mInterstitialAd?.show(activity)
    }

    fun hasAdAvailable() = mInterstitialAd != null

}