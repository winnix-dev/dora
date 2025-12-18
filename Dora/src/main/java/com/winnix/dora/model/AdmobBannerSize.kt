package com.winnix.dora.model

sealed class AdmobBannerSize {
    object Banner50 : AdmobBannerSize()
    object Adaptive : AdmobBannerSize()
}