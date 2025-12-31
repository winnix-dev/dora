package com.winnix.dora.model

import com.google.android.gms.ads.interstitial.InterstitialAd

sealed class InterstitialResult {
    object Idle : InterstitialResult()
    object Loading : InterstitialResult()
    object Failed: InterstitialResult()
    data class Success(val ad: InterstitialAd) : InterstitialResult()
}