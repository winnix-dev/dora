package com.winnix.dora.manager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.winnix.dora.admob_manager.AdmobInterstitial
import com.winnix.dora.callback.LoadInterstitialCallback
import com.winnix.dora.callback.ShowInterstitialCallback
import com.winnix.dora.model.DoraAdError
import com.winnix.dora.model.InterstitialState
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
                AdmobInterstitial.interstitialAd.first { it is InterstitialState.Success || it is InterstitialState.Failed }

                if (AdmobInterstitial.interstitialAd.value is InterstitialState.Failed) {
                    YandexInterstitial.interstitialAd.first { it is InterstitialState.Success || it is InterstitialState.Failed }
                }
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

                    override fun onShowFailed(adError: DoraAdError) {
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

                                override fun onShowFailed(adError: DoraAdError) {
                                    callback.onShowFailed(adError)
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
                it is InterstitialState.Success || it is InterstitialState.Failed
            }
            if(AdmobInterstitial.interstitialAd.value is InterstitialState.Success) {
                return@withTimeoutOrNull true
            }
            YandexInterstitial.interstitialAd.first {
                it is InterstitialState.Success || it is InterstitialState.Failed
            }
            return@withTimeoutOrNull YandexInterstitial.interstitialAd.value is InterstitialState.Success
        }

        return admobResult == true
    }
    fun isAdmobAlready() : Boolean = AdmobInterstitial.interstitialAd.value is InterstitialState.Success
}