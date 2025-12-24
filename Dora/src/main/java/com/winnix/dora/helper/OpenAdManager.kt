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
import com.winnix.dora.admob_manager.AdmobOpenAppManager
import com.winnix.dora.callback.OpenAdCallback
import com.winnix.dora.callback.ShowAdCallback
import com.winnix.dora.model.AdUnit
import com.winnix.dora.yandex_manager.YandexOpenApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OpenAdManager(
    val application: Application,
    val callback: OpenAdCallback
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private var currentActivity: Activity? = null

    private val _showState = MutableStateFlow(false)
    val showState = _showState.asStateFlow()

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        YandexOpenApp.int(application)

        loadAd()
    }

    fun loadAd() {
        AdmobOpenAppManager.loadAd(application)
        YandexOpenApp.loadAd()
    }

    fun showAdIfAvailable(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        Log.d("DORA OpenAd", "showAdIfAvailable: ")
        if (activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        if (_showState.value || !callback.canShow()) {
            onComplete()
            return
        }

        AdmobOpenAppManager.showAdIfAvailable(
            activity,
            callback = object : ShowAdCallback {
                override fun onDismiss() {
                    onComplete()
                    _showState.update { false }
                    loadAd()
                }

                override fun onShowSuccess() {
                    _showState.update { true }
                }

                override fun onShowFail() {
                    YandexOpenApp.showAd(
                        activity,
                        callback = object : ShowAdCallback {
                            override fun onShowSuccess() {
                                _showState.update { true }
                            }

                            override fun onShowFail() {
                                _showState.update { false }
                                onComplete()
                                loadAd()
                            }

                            override fun onDismiss() {
                                _showState.update { false }
                                onComplete()
                                loadAd()
                            }
                        }
                    )
                }
            }
        )
    }

    fun isAvailable(): Boolean {
        return AdmobOpenAppManager.isAvailable() || YandexOpenApp.isAdAvailable()
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