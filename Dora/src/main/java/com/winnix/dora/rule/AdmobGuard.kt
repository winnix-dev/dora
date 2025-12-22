package com.winnix.dora.rule

import com.winnix.dora.model.AdUnit

class AdmobGuard {
    private val adRules = mutableListOf<AdmobRule>()
    private val showAdRules = mutableListOf<AdmobRule>()

    fun addRule(rule: AdmobRule) {
        adRules.add(rule)
    }

    fun addShowRule(rule: AdmobRule) {
        showAdRules.add(rule)
    }

    fun canLoadAd(ad: AdUnit) : Boolean {
        return adRules.all { it.checking(ad) }
    }

    fun canShowAd(ad: AdUnit) : Boolean {
        return adRules.all { it.checking(ad) } && showAdRules.all { it.checking(ad) }
    }
}