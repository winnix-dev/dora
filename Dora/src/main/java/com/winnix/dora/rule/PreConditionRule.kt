package com.winnix.dora.rule

import com.winnix.dora.model.AdmobUnit

class PreConditionRule(
    val condition: (ad: AdmobUnit) -> Boolean
) : AdmobRule {
    override fun checking(ad: AdmobUnit): Boolean {
        return condition(ad)
    }
}