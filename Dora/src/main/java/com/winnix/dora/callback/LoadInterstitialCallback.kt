package com.winnix.dora.callback

interface LoadInterstitialCallback {
    fun onBeginLoad() {}
    fun onFailed() {}
    fun onLoaded() {}
}