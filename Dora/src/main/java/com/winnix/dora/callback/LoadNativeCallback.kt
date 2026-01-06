package com.winnix.dora.callback

import com.google.android.gms.ads.nativead.NativeAd
import com.winnix.dora.helper.AdProvider

interface LoadNativeCallback {
    fun onLoad(adProvider: AdProvider)
    fun loadSuccess(adProvider: AdProvider) {}
    fun loadFailed(adProvider: AdProvider, errorCode: Int, errorMessage: String) {}
}