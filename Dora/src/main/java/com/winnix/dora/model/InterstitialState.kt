package com.winnix.dora.model

import com.google.android.gms.ads.interstitial.InterstitialAd as AdMobInter
import com.yandex.mobile.ads.interstitial.InterstitialAd as YandexInter

sealed class InterstitialResult {
    data class AdMob(val ad: AdMobInter) : InterstitialResult()
    data class Yandex(val ad: YandexInter) : InterstitialResult()
}

sealed class InterstitialState {
    object Idle : InterstitialState()
    object Loading : InterstitialState()
    object Failed: InterstitialState()
    data class Success(val data: InterstitialResult) : InterstitialState()
}