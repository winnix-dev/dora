package com.winnix.dora.admob_manager

import com.winnix.dora.R

sealed class NativeLayout {
    object Native50 : NativeLayout()
    object Native150 : NativeLayout()
    object Native250 : NativeLayout()
    object NativeCollapsible : NativeLayout()
    object NativeFull : NativeLayout()
    object NativeFullWithNextButton : NativeLayout()
    data class Custom(val layout: Int) : NativeLayout()

    fun getLayoutAd() : Int {
        return when (this) {
            Native50 -> R.layout.dora_layout_ads_native_50
            Native150 -> R.layout.dora_layout_ads_native_150
            Native250 -> R.layout.dora_layout_ads_native_250
            NativeFull -> R.layout.dora_layout_ads_native_full
            NativeFullWithNextButton -> R.layout.dora_layout_ads_native_full_with_next_btn
            NativeCollapsible -> R.layout.dora_layout_ads_native_collapsible
            is Custom -> this.layout
        }
    }
}