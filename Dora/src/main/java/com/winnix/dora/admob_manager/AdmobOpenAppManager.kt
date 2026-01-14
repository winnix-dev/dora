package com.winnix.dora.admob_manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.winnix.dora.Dora
import com.winnix.dora.Dora.ensureInitialized
import com.winnix.dora.callback.ShowAdCallback
import com.winnix.dora.helper.DoraLogger
import com.winnix.dora.model.AdType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdmobOpenAppManager {

    private var openAd: AppOpenAd? = null
    private var isLoading = false
    private var loadTime = 0L

    var id: String = ""

    fun loadAd(
        context: Context,
    ) {
        if((System.currentTimeMillis() - loadTime) > 4 * 3600000) {
            openAd = null
        }

        if (isLoading || openAd != null) return

        isLoading = true

        CoroutineScope(Dispatchers.Main).launch {
            ensureInitialized()

            AppOpenAd.load(
                context.applicationContext,
                id,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(p0: AppOpenAd) {
                        DoraLogger.logAdMobLoadSuccess(AdType.OpenApp, id)
                        isLoading = false
                        openAd = p0
                        loadTime = System.currentTimeMillis()
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        DoraLogger.logAdMobLoadFail(AdType.OpenApp, id, p0)
                        isLoading = false
                    }
                }
            )
        }
    }

    fun isAvailable(): Boolean {
        return openAd != null && (System.currentTimeMillis() - loadTime) < 4 * 3600000
    }

    fun showAdIfAvailable(
        activity: Activity,
        callback: ShowAdCallback
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            callback.onDismiss()
            return
        }

        if (!isAvailable()) {
            loadAd(activity)
            callback.onShowFail()
            return
        }

        openAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                openAd = null
                loadAd(activity)
                callback.onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                DoraLogger.logAdMobShowFail(AdType.OpenApp, p0)
                openAd = null
                loadAd(activity)
                callback.onShowFail()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("Dora", "onAdShowedFullScreenContent")
                callback.onShowSuccess()
            }
        }

        openAd?.show(activity)

    }
}