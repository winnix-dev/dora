package com.winnix.dora.yandex_manager

sealed class YandexNativeLayout {
    object Native50 : YandexNativeLayout()
    object Native250 : YandexNativeLayout()
}