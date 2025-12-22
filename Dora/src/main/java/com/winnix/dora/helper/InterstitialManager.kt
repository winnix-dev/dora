package com.winnix.dora.helper

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.winnix.dora.Dora
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.model.AdmobUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object InterstitialManager {
    private val _adState = MutableStateFlow<InterstitialAd?>(null)
    val adState = _adState.asStateFlow()

    private var idList = listOf<AdmobUnit>()
    private var currentIndex = 0

    private var retryTime = 4000L

    private var isLoading = false
    private var isWaiting = false

    var callback: LoadInterstitialCallback? = null

    fun setUp(
        listAd: List<AdmobUnit>,
        callback: LoadInterstitialCallback?,
        context: Context
    ) {
        idList = listAd
        this.callback = callback

        loadAd(context)
    }

    fun loadAd(
        context: Context,
    ) {
        if (isLoading || _adState.value != null || idList.isEmpty() || isWaiting) {
            return
        }

        callback?.onBeginLoad()

        val adRequest = AdRequest.Builder().build()

        val ad = idList[currentIndex % idList.size]

//        Log.d("TAGG", "loadAd: ${Dora.getAdId(ad)}")

        InterstitialAd.load(
            context.applicationContext,
            Dora.getAdId(ad),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: InterstitialAd) {
                    isLoading = false
                    _adState.update { p0 }
                    callback?.onLoaded()
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    isLoading = false
                    callback?.onFailed(p0)

                    Log.e("Dora", "Inters Failed: $ad $p0")

                    CoroutineScope(Dispatchers.Main).launch {
                        isWaiting = true
                        delay(retryTime)
                        currentIndex ++
                        isWaiting = false
                        loadAd(context)
                    }
                }
            }
        )
    }

    fun onConsumed(
        context: Context,
    ) {
        _adState.update { null }
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000L)
            loadAd(context)
        }
    }

    fun isAvailable() : Boolean = _adState.value != null

}