package com.winnix.dora.callback

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd

interface LoadInterstitialCallback {
    fun onFailed(p0: LoadAdError) {}
    fun onLoaded(p0: InterstitialAd) {}
}