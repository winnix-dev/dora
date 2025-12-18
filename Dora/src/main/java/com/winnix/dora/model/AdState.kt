package com.winnix.dora.model

data class AdState(
    val isIntersShowing: Boolean = false,
    val isOpenAppShowing: Boolean = false,
    val isNativeFullShowing: Boolean = false,
) {
    val isShowingAdFullscreen get() = isIntersShowing || isOpenAppShowing || isNativeFullShowing
}