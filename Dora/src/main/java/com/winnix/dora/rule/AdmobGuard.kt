package com.winnix.dora.rule

class AdmobGuard internal constructor(
    val interstitialRule: AdmobRule? = null,
    val nativeRule: AdmobRule? = null,
    val bannerRule: AdmobRule? = null,
    val openAppRule: AdmobRule? = null,
    val nativeFullRule: AdmobRule? = null,
    val adRule: AdmobRule? = null
) {
    fun checkAd() : Boolean = (adRule?.checking())?.invoke() != false
    fun checkInters() : Boolean = (interstitialRule?.checking())?.invoke() != false && checkAd()
    fun checkNative() : Boolean = (nativeRule?.checking())?.invoke() != false && checkAd()
    fun checkBanner() : Boolean = (bannerRule?.checking())?.invoke() != false && checkAd()
    fun checkNativeFull() : Boolean = (nativeFullRule?.checking())?.invoke() != false && checkAd()
    fun checkOpenApp() : Boolean = (openAppRule?.checking())?.invoke() != false && checkAd()

    class Builder {
        private var interstitialRule: AdmobRule? = null
        private var nativeRule: AdmobRule? = null
        private var bannerRule: AdmobRule? = null
        private var openAppRule: AdmobRule? = null
        private var adRule: AdmobRule? = null
        private var nativeFullRule: AdmobRule? = null

        fun setInterstitialRule(interstitialRule: AdmobRule) = apply {
            this.interstitialRule = interstitialRule
        }

        fun setNativeRule(nativeRule: AdmobRule) = apply {
            this.nativeRule = nativeRule
        }

        fun setBannerRule(bannerRule: AdmobRule) = apply {
            this.bannerRule = bannerRule
        }

        fun setOpenAppRule(openAppRule: AdmobRule) = apply {
            this.openAppRule = openAppRule
        }

        fun setAdRule(adRule: AdmobRule) = apply {
            this.adRule = adRule
        }

        fun setNativeFullRule(adRule: AdmobRule) = apply {
            this.nativeFullRule = adRule
        }

        fun build() : AdmobGuard {
            return AdmobGuard(
                interstitialRule = interstitialRule,
                nativeRule = nativeRule,
                bannerRule = bannerRule,
                openAppRule = openAppRule,
                nativeFullRule = nativeFullRule,
                adRule = adRule
            )
        }
    }
}