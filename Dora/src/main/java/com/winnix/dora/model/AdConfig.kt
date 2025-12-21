package com.winnix.dora.model

data class AdConfig (
    val isDebug: Boolean = true,
    val intersTimeout: Long = 6000L,
    val maxNativeCache: Int = 2,
    val nativeTimeInterval: Long = 3000L,
)