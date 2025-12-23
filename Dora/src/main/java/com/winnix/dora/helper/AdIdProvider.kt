package com.winnix.dora.helper

import com.winnix.dora.model.AdUnit

enum class AdProvider {
    AD_MOB, YANDEX
}

internal object AdIdProvider {
    var isDebug = false
    var idDictionary: Map<String, String> = mapOf()

    fun getAdId(
        adUnit: AdUnit,
        adProvider: AdProvider
    ) : String {
        return if (isDebug) {
            when(adProvider) {
                AdProvider.AD_MOB -> {
                    adUnit.adType.getAdmobDebugId()
                }
                AdProvider.YANDEX -> {
                    adUnit.adType.getYandexDebugId()
                }
            }
        } else {
            idDictionary[adUnit.name] ?: adUnit.id
        }
    }
}