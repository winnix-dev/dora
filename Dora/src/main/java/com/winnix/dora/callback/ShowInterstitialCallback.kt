package com.winnix.dora.callback

import com.google.android.gms.ads.AdError

interface ShowInterstitialCallback {
    fun onDismiss()
    fun onShow(){}
    fun onShowFailed() {}
    fun onImpression() {}
}