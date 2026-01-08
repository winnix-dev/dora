package com.winnix.dora.model

import com.google.android.gms.ads.AdSize

sealed class AdmobBannerSize {
    data class FixedSize(val size: AdSize) : AdmobBannerSize()
    object Collapsible : AdmobBannerSize()
    data class InlineAdaptive(val height: Int) : AdmobBannerSize()
}