package com.winnix.dora.model

sealed class AdmobType {
    object Inters : AdmobType()
    object Native : AdmobType()
    object Banner : AdmobType()
    object OpenApp : AdmobType()

    fun getDebugId() : String {
        return when(this) {
            Banner -> "ca-app-pub-3940256099942544/9214589741"
            Inters -> "ca-app-pub-3940256099942544/1033173712"
            Native -> "ca-app-pub-3940256099942544/2247696110"
            OpenApp -> "ca-app-pub-3940256099942544/9257395921"
        }
    }
}