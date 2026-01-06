package com.winnix.dora.callback

import com.winnix.dora.helper.AdProvider

interface LoadInterstitialCallback {
    fun onBeginLoad(adProvider: AdProvider) {}
    fun onFailed(adProvider: AdProvider, errorCode: Int, errorMessage: String) {}
    fun onLoaded(adProvider: AdProvider) {}
}