package com.winnix.dora.model

import com.yandex.mobile.ads.interstitial.InterstitialAd

sealed class YandexInterstitialResult {
    object Idle : YandexInterstitialResult()
    object Loading : YandexInterstitialResult()
    object Failed: YandexInterstitialResult()
    data class Success(val ad: InterstitialAd) : YandexInterstitialResult()
}