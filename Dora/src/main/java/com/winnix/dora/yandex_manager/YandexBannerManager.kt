package com.winnix.dora.yandex_manager

import android.app.Activity
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.winnix.dora.callback.LoadBannerCallback
import com.winnix.dora.helper.AdProvider
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData

object YandexBannerManager {
    fun loadBanner(
        activity: Activity,
        container: ViewGroup,
        id: String,
        lifecycleOwner: LifecycleOwner,
        callback: LoadBannerCallback?
    ) {

        val banner = BannerAdView(activity)

        val adWidth = (activity.resources.displayMetrics.widthPixels / activity.resources.displayMetrics.density).toInt()
        val maxAdHeight = 100

        val adSize = BannerAdSize.inlineSize(activity, adWidth, maxAdHeight)
        banner.apply {
            setAdSize(adSize)
            setAdUnitId(id)
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdClicked() {

                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    callback?.onLoadFailed(
                        AdProvider.YANDEX,
                        errorMessage = error.description,
                        errorCode = error.code
                    )
                }

                override fun onAdLoaded() {
                    callback?.onLoadSuccess(
                        adProvider = AdProvider.YANDEX
                    )
                    if(activity.isDestroyed) {
                        banner.destroy()
                        return
                    }

                    container.removeAllViews()
                    container.addView(banner)

                    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            banner.destroy()
                        }
                    })
                }

                override fun onImpression(impressionData: ImpressionData?) {}

                override fun onLeftApplication() {}

                override fun onReturnedToApplication() {}

            })
            callback?.onLoad(AdProvider.YANDEX)
            loadAd(AdRequest.Builder().build())
        }
    }
}