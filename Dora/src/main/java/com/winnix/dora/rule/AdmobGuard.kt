package com.winnix.dora.rule

import com.winnix.dora.model.AdmobUnit

class AdmobGuard {
    private val adRules = mutableListOf<AdmobRule>()
    private val showAdRules = mutableListOf<AdmobRule>()

    fun addRule(rule: AdmobRule) {
        adRules.add(rule)
    }

    fun addShowRule(rule: AdmobRule) {
        showAdRules.add(rule)
    }

    fun canLoadAd(ad: AdmobUnit) : Boolean {
        return adRules.all { it.checking(ad) }
    }

    fun canShowAd(ad: AdmobUnit) : Boolean {
        return adRules.all { it.checking(ad) } && showAdRules.all { it.checking(ad) }
    }
}