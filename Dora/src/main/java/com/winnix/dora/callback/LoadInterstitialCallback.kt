package com.winnix.dora.callback

import com.winnix.dora.model.DoraAdError

interface LoadInterstitialCallback {
    fun onBeginLoad() {}
    fun onFailed(adError: DoraAdError) {}
    fun onLoaded() {}
}