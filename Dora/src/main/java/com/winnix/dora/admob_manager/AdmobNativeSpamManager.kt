package com.winnix.dora.admob_manager

import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.flow.MutableStateFlow

object AdmobNativeSpamManager {
    private val _adState = MutableStateFlow<NativeAd?>(null)

    private var currentIndex = 0
    var idList = listOf<String>()

    fun loadNative(
        
    ) {

    }

}