package com.winnix.dora.model

sealed class AdType {
    object Inters : AdType()
    object Native : AdType()
    object Banner : AdType()
    object OpenApp : AdType()
    object Rewarded : AdType()

    fun getAdmobDebugId() : String {
        return when(this) {
            Banner -> "ca-app-pub-3940256099942544/9214589741"
            Inters -> "ca-app-pub-3940256099942544/1033173712"
            Native -> "ca-app-pub-3940256099942544/2247696110"
            OpenApp -> "ca-app-pub-3940256099942544/9257395921"
            Rewarded -> "ca-app-pub-3940256099942544/5224354917"
        }
    }

    fun getYandexDebugId() : String {
        return when(this) {
            Banner -> "demo-banner-yandex"
            Inters -> "demo-interstitial-yandex"
            Native -> "demo-native-slider-yandex"
            OpenApp -> "demo-appopenad-yandex"
            Rewarded -> "demo-rewarded-yandex"
        }
    }

}