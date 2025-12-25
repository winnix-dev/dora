package com.winnix.dora.rule

interface AdmobRule {
    fun checking() : () -> Boolean
    fun onAdShow() = {}
}