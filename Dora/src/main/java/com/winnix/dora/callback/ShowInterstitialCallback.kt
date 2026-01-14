package com.winnix.dora.callback

import com.winnix.dora.model.DoraAdError

interface ShowInterstitialCallback {
    fun onDismiss()
    fun onShow(){}
    fun onShowFailed(adError: DoraAdError) {}
    fun onImpression() {}
}