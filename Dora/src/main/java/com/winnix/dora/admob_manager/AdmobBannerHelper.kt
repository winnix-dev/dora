package com.winnix.dora.admob_manager

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.winnix.dora.callback.LoadBannerCallback
import com.winnix.dora.model.AdmobBannerSize

object AdmobBannerHelper {
    fun loadBanner(
        id: String,
        activity: Activity,
        container: ViewGroup,
        adSize: AdmobBannerSize,
        lifecycleOwner: LifecycleOwner,
        callback: LoadBannerCallback
    ) {
        val adView = AdView(activity)

        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                Log.e("Dora", "load Banner fail: $p0")
                callback.onLoadFailed()
            }

            override fun onAdLoaded() {
                callback.onLoadSuccess()

                lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onResume(owner: LifecycleOwner) {
                        adView.resume()
                    }

                    override fun onPause(owner: LifecycleOwner) {
                        adView.pause()
                    }

                    override fun onDestroy(owner: LifecycleOwner) {
                        adView.destroy()
                    }
                })
            }
        }
        adView.adUnitId = id

        var extras : Bundle? = null

        when(adSize) {
            AdmobBannerSize.Adaptive -> {
                adView.setAdSize(getAdaptiveAdSize(activity, container))
                extras = Bundle()
                extras.putString("collapsible", "bottom")
            }
            AdmobBannerSize.Banner50 -> {
                adView.setAdSize(AdSize.BANNER)
            }
        }

        container.removeAllViews()
        container.addView(adView)

        val adRequest = if(extras == null) {
            AdRequest.Builder()
                .build()
        }
        else {
            AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()
        }

        adView.loadAd(adRequest)

    }

    private fun getAdaptiveAdSize(activity: Activity, container: ViewGroup): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        val frameWidth = if (container.width > 0) container.width.toFloat() else displayMetrics.widthPixels.toFloat()
        val adWidth = (frameWidth / displayMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

}