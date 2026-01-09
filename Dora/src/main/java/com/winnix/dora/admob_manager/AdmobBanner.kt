package com.winnix.dora.admob_manager

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.WindowMetrics
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.winnix.dora.Dora
import com.winnix.dora.callback.LoadBannerCallback
import com.winnix.dora.model.AdmobBannerSize
import kotlinx.coroutines.launch

object AdmobBanner {
    fun loadBanner(
        id: String,
        activity: Activity,
        container: ViewGroup,
        adSize: AdmobBannerSize,
        lifecycleOwner: LifecycleOwner,
        callback: LoadBannerCallback
    ) {
        lifecycleOwner.lifecycleScope.launch {
            Dora.ensureInitialized()

            val adView = AdView(activity)

            var scrollView: NestedScrollView? = null

            val adLifecycleObserver = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    adView.resume()
                }

                override fun onPause(owner: LifecycleOwner) {
                    adView.pause()
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    adView.destroy()
                }
            }

            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    Log.e("Dora", "load Banner fail: $p0")
                    lifecycleOwner.lifecycle.removeObserver(adLifecycleObserver)
                    callback.onLoadFailed()
                }

                override fun onAdLoaded() {

                    if (adSize is AdmobBannerSize.InlineAdaptive) {
                        val maxHeight =
                            (adSize.height * activity.resources.displayMetrics.density).toInt()

                        scrollView?.apply {
                            val params = layoutParams

                            params.height = if (maxHeight > (adView.adSize?.getHeightInPixels(
                                    activity
                                ) ?: 0)
                            ) ViewGroup.LayoutParams.WRAP_CONTENT else maxHeight

                            layoutParams = params
                        }
                    }
                    callback.onLoadSuccess()
                }
            }

            adView.adUnitId = id

            var extras: Bundle? = null

            when (adSize) {
                AdmobBannerSize.Collapsible -> {
                    adView.setAdSize(getAdaptiveAdSize(activity, container))
                    extras = Bundle()
                    extras.putString("collapsible", "bottom")
                }

                is AdmobBannerSize.FixedSize -> {
                    adView.setAdSize(adSize.size)
                }

                is AdmobBannerSize.InlineAdaptive -> {
                    val displayMetrics = activity.resources.displayMetrics
                    val adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val windowMetrics: WindowMetrics =
                            activity.windowManager.currentWindowMetrics
                        windowMetrics.bounds.width()
                    } else {
                        displayMetrics.widthPixels
                    }
                    val adWidth = (adWidthPixels / displayMetrics.density).toInt()

                    val size =
                        AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, adWidth)
                    adView.setAdSize(size)

                    scrollView = NestedScrollView(activity).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )

                        isFillViewport = true
                    }

                    container.removeAllViews()
                    container.addView(scrollView)
                    scrollView.addView(adView)
                }
            }

            container.setBackgroundColor("#FFFFFF".toColorInt())

            if (adSize !is AdmobBannerSize.InlineAdaptive) {
                container.removeAllViews()
                container.addView(adView)
            }

            val adRequest = if (extras == null) {
                AdRequest.Builder()
                    .build()
            } else {
                AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                    .build()
            }

            lifecycleOwner.lifecycle.addObserver(adLifecycleObserver)

            adView.loadAd(adRequest)
        }
    }

    private fun getAdaptiveAdSize(activity: Activity, container: ViewGroup): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        val frameWidth = if (container.width > 0) container.width.toFloat() else displayMetrics.widthPixels.toFloat()
        val adWidth = (frameWidth / displayMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

}