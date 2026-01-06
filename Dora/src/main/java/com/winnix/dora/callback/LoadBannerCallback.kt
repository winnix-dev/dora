package com.winnix.dora.callback

import com.winnix.dora.helper.AdProvider

interface LoadBannerCallback {
    fun onLoad(adProvider: AdProvider)
    fun onLoadSuccess (adProvider: AdProvider) {}
    fun onLoadFailed(adProvider: AdProvider, errorCode: Int, errorMessage: String) {}
}