package com.winnix.adsdk

import android.app.Application
import android.util.Log
import com.winnix.dora.Dora

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Dora.registerOpenAd(
            this,
            "ca-app-pub-3940256099942544/9257395921",
            "demo-appopenad-yandex"
        ) {
            Log.d("TAGG", "check Open ad ${AdConfig.canShowOpenAd}")
            AdConfig.canShowOpenAd
        }
    }
}