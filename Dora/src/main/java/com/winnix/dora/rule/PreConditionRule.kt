package com.winnix.dora.rule

import com.winnix.dora.model.AdmobUnit

class NoConsecutiveCondition() : AdmobRule {
    companion object {
        var isShowIntersBefore = false
    }

    private val canShow get() = isShowIntersBefore

    override fun checking(ad: AdmobUnit): Boolean {
        return canShow
    }
}