package com.winnix.dora.yandex_manager

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.winnix.dora.Dora
import com.winnix.dora.R
import com.winnix.dora.model.AdUnit
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdRequestConfiguration
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object YandexNativeManger {
    private var isWaiting = false
    private var adUnit : AdUnit? = null
    private var mNativeAd: NativeAd? = null
    private var isNativeLoading = false

    fun loadNativeAd(
        context: Context,
    ) {
        if(mNativeAd != null || isNativeLoading || adUnit == null || isWaiting) {
            return
        }

        val loader = NativeAdLoader(context.applicationContext)

        val config = NativeAdRequestConfiguration.Builder(Dora.getYandexId(adUnit!!)).build()

        loader.setNativeAdLoadListener(object : NativeAdLoadListener {
            override fun onAdLoaded(nativeAd: NativeAd) {
                mNativeAd = nativeAd
                isNativeLoading = false
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.e("Dora", "Load Yandex Native Failed: ${error.description}")
                mNativeAd = null
                isNativeLoading = false

                isWaiting = true

                CoroutineScope(Dispatchers.Main).launch {
                    delay(6500)
                    isWaiting = false
                    loadNativeAd(context)
                }
            }
        })
        isNativeLoading = true
        loader.loadAd(config)
    }

    fun showNativeAd(
        viewGroup: ViewGroup,
        inflater: LayoutInflater
    ) {
        mNativeAd?.let { nativeAd ->
            val itemView = inflater.inflate(R.layout.dora_yandex_native_ad_250, viewGroup, false) as NativeAdView

            val binder = NativeAdViewBinder.Builder(itemView)
                .setCallToActionView(itemView.findViewById(R.id.ad_call_to_action))
                .setDomainView(itemView.findViewById(R.id.ad_domain))
                .setIconView(itemView.findViewById(R.id.ad_icon))
                .setMediaView(itemView.findViewById(R.id.ad_media))
                .setTitleView(itemView.findViewById(R.id.ad_title))
                .setSponsoredView(itemView.findViewById(R.id.ad_sponsored))
                .setFeedbackView(itemView.findViewById(R.id.ad_feedback))
                .setWarningView(itemView.findViewById(R.id.ad_warning))
                .build()

            try {
                nativeAd.bindNativeAd(binder)

                viewGroup.removeAllViews()
                viewGroup.addView(itemView)
                viewGroup.visibility = View.VISIBLE

                mNativeAd = null
                isNativeLoading = false

                loadNativeAd(viewGroup.context)

            } catch (e: Exception) {
                Log.e("Dora", "Failed to bind fixed size native ad $e")
            }
        }
    }
}