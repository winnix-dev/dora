package com.winnix.dora.rule

import com.winnix.dora.model.AdUnit

interface AdmobRule {
    fun checking(ad: AdUnit) : Boolean
    fun onAdShow(ad: AdUnit) = {}
}