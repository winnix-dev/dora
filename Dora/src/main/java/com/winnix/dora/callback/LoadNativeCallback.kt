package com.winnix.dora.callback

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

interface LoadNativeCallback {
    fun loadSuccess(ad: NativeAd) {}
    fun loadFailed(e: LoadAdError) {}
}