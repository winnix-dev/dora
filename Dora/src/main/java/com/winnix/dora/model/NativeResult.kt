package com.winnix.dora.model

import com.google.android.gms.ads.nativead.NativeAd


sealed class NativeResult {
    object Idle : NativeResult()
    object Loading : NativeResult()
    object Failed: NativeResult()
    data class Success(val ad: NativeAd) : NativeResult()
}