package com.winnix.dora.callback

interface OpenAdCallback {
    fun canShow() : Boolean
    fun onShowAd()
    fun onDismiss()
}