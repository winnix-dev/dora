package com.winnix.dora.callback

interface ShowInterstitialCallback {
    fun onDismiss()
    fun onShow(){}
    fun onShowFailed() {}
    fun onImpression() {}
}