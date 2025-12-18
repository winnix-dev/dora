package com.winnix.dora.callback

import com.google.android.gms.ads.nativead.NativeAd

interface FetchNativeCallback {
    fun onSuccess(ad: NativeAd)
    fun onFailed()
}