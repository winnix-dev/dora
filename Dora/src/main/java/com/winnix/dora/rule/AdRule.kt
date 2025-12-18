package com.winnix.dora.rule

import com.winnix.dora.model.AdmobUnit

interface AdmobRule {
    fun checking(ad: AdmobUnit) : Boolean
    fun onAdShow(ad: AdmobUnit) = {}
}