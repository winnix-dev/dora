package com.winnix.dora.admob_manager

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.helper.AdIdProvider
import com.winnix.dora.helper.AdProvider
import com.winnix.dora.helper.LoadAdEnum
import com.winnix.dora.model.AdUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object AdmobInterstitialManager {
    private val _adState = MutableStateFlow<InterstitialAd?>(null)
    val adState = _adState.asStateFlow()

    private val _loadState = MutableStateFlow(LoadAdEnum.IDLE)
    val loadState = _loadState.asStateFlow()

    private var idList = listOf<AdUnit>()
    private var currentIndex = 0

    private var retryTime = 4000L

    private var isLoading = false
    private var isWaiting = false

    var callback: LoadInterstitialCallback? = null

    fun setUp(
        listAd: List<AdUnit>,
        context: Context,
        callback: LoadInterstitialCallback?,
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

        isLoading = true
        _loadState.update { LoadAdEnum.LOADING }
        InterstitialAd.load(
            context.applicationContext,
            AdIdProvider.getAdId(ad, AdProvider.AD_MOB),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: InterstitialAd) {
                    isLoading = false
                    _adState.update { p0 }
                    _loadState.update { LoadAdEnum.SUCCESS }

                    callback?.onLoaded()
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    isLoading = false
                    callback?.onFailed(p0)

                    Log.e("Dora", "Inters Failed: $ad $p0")
                    _loadState.update { LoadAdEnum.FAILED }

                    CoroutineScope(Dispatchers.Main).launch {
                        isWaiting = true
                        if (p0.code == 0) {
                            delay(10_000L)
                        }
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