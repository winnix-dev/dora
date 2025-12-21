package com.winnix.dora.callback

import com.google.android.gms.ads.LoadAdError

interface LoadInterstitialCallback {
    fun onBeginLoad() {}
    fun onFailed(p0: LoadAdError) {}
    fun onLoaded() {}
}