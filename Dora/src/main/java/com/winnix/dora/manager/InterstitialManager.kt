package com.winnix.dora.manager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.winnix.dora.admob_manager.AdmobInterstitial
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.model.InterstitialResult
import com.winnix.dora.model.YandexInterstitialResult
import com.winnix.dora.yandex_manager.YandexInterstitial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal object InterstitialManager {
    fun loadInter(
        context: Context,
        id: String,
        yandexId: String? = null,
        listener: LoadInterstitialCallback? = null
    ) {
        AdmobInterstitial.loadAd(
            context = context,
            id = id,
            listener = listener
        )
        yandexId?.let { id ->
            YandexInterstitial.loadAd(
                context = context,
                id = id,
                listener = listener,
            )
        }
    }
    fun showInter(
        activity: AppCompatActivity,
        timeoutLong: Long,
        callback: ShowInterstitialCallback
    ) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
            withTimeoutOrNull(timeoutLong) {
                AdmobInterstitial.interstitialAd.first { it is InterstitialResult.Success }
            }
            AdmobInterstitial.showAd(
                activity,
                listener = object : ShowInterstitialCallback {
                    override fun onDismiss() {
                        callback.onDismiss()
                    }

                    override fun onImpression() {
                        callback.onImpression()
                    }

                    override fun onShow() {
                        callback.onShow()
                    }

                    override fun onShowFailed() {
                        YandexInterstitial.showAd(
                            activity = activity,
                            listener = object : ShowInterstitialCallback {
                                override fun onDismiss() {
                                    callback.onDismiss()
                                }

                                override fun onShow() {
                                    callback.onShow()
                                }

                                override fun onImpression() {
                                    callback.onImpression()
                                }

                                override fun onShowFailed() {
                                    callback.onShowFailed()
                                    callback.onDismiss()
                                }
                            }
                        )
                    }
                }
            )
        }
    }

    suspend fun waitForInterstitialWithTimeout(timeoutLong: Long) : Boolean {
        val admobResult = withTimeoutOrNull(timeoutLong) {
            AdmobInterstitial.interstitialAd.first {
                it is InterstitialResult.Success || it is InterstitialResult.Failed
            }
            if(AdmobInterstitial.interstitialAd.value is InterstitialResult.Success) {
                return@withTimeoutOrNull true
            }
            YandexInterstitial.interstitialAd.first {
                it is YandexInterstitialResult.Success || it is YandexInterstitialResult.Failed
            }
            return@withTimeoutOrNull YandexInterstitial.interstitialAd.value is YandexInterstitialResult.Success
        }

        return admobResult == true
    }

    fun isAdmobAlready() : Boolean = AdmobInterstitial.interstitialAd.value is InterstitialResult.Success
}