package com.winnix.dora.model

import com.winnix.dora.rule.AdmobGuard

data class AdConfig (
    val isDebug: Boolean = true,
    val intersTimeout: Long = 6000L,
    val maxNativeCache: Int = 2,
    val nativeTimeInterval: Long = 3000L,
    val admobGuard: AdmobGuard? = null
)