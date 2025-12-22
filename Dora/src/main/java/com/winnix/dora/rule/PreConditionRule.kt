package com.winnix.dora.rule

import com.winnix.dora.model.AdUnit

class NoConsecutiveCondition() : AdmobRule {
    companion object {
        var isShowIntersBefore = false
    }

    private val canShow get() = isShowIntersBefore

    override fun checking(ad: AdUnit): Boolean {
        return canShow
    }
}