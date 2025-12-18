package com.winnix.dora.helper

sealed class NativeLayout {
    object Native50 : NativeLayout()
    object Native150 : NativeLayout()
    object Native250 : NativeLayout()
    object NativeCollapsible : NativeLayout()
    object NativeFull : NativeLayout()
    object NativeFullWithNextButton : NativeLayout()
    data class Custom(val layout: Int) : NativeLayout()
}