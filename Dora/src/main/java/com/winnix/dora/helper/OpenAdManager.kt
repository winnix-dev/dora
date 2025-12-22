package com.winnix.dora.helper

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.winnix.dora.Dora
import com.winnix.dora.callback.OpenAdCallback
import com.winnix.dora.model.AdmobUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OpenAdManager(
    val application: Application,
    var adId: AdmobUnit,
    val callback: OpenAdCallback
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private var currentActivity: Activity? = null
    private var openAd: AppOpenAd? = null
    private var isLoading = false
    private var loadTime = 0L
    private var isShowingAd = false

    private val _showState = MutableStateFlow(false)
    val showState = _showState.asStateFlow()

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        loadAd()
    }

    fun loadAd() {
        if(isLoading || openAd != null) return

        isLoading = true

        AppOpenAd.load(
            application,
            Dora.getAdId(adId),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(p0: AppOpenAd) {
                    Log.d("TAGG", "onAdLoaded: $p0")
                    isLoading = false
                    openAd = p0
                    loadTime = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    Log.d("TAGG", "onAdFailed: $p0")
                    isLoading = false
                }
            }
        )
    }

    fun showAdIfAvailable(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        if(activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        if(isShowingAd || !callback.canShow()) {
            onComplete()
            return
        }

        if(!isAvailable()) {
            loadAd()
            onComplete()
            return
        }

        openAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingAd = false
                _showState.update { false }
                openAd = null
                loadAd()
                onComplete()
                callback.onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Log.d("Dora", "onAdFailedToShowFullScreenContent $p0")
                isShowingAd = false
                _showState.update { false }
                openAd = null
                loadAd()
                callback.onDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("Dora", "onAdShowedFullScreenContent")
                isShowingAd = true
                _showState.update { true }
                callback.onShowAd()
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            delay(200)
            openAd?.show(activity)
        }
    }

    fun isAvailable() : Boolean {
        return openAd != null && (System.currentTimeMillis() - loadTime) < 4 * 3600000
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        currentActivity?.let {
            showAdIfAvailable(it) {}
        }
    }

    override fun onActivityStarted(p0: Activity) {
        currentActivity = p0
    }

    override fun onActivityResumed(p0: Activity) {
        currentActivity = p0
    }

    override fun onActivityDestroyed(p0: Activity) {
        currentActivity = null
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

}