package com.winnix.dora.callback

import com.google.android.gms.ads.LoadAdError
import com.winnix.dora.model.AdmobUnit

interface ShowNativeCallback {
    fun onSuccess()
    fun onFailed()
}