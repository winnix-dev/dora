package com.winnix.adsdk

import android.app.Application
import com.winnix.dora.Dora

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Dora.registerOpenAd(this)
    }
}