package com.winnix.dora.yandex_manager

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.winnix.dora.R
import com.winnix.dora.callback.LoadNativeCallback
import com.winnix.dora.helper.DoraLogger
import com.winnix.dora.model.AdType
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdRequestConfiguration
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal object YandexNativeManger {
    private val _mNativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAdFlow = _mNativeAd.asStateFlow()
    private var isNativeLoading = false

    fun loadNativeAd(
        context: Context,
        id: String
    ) {
        if (_mNativeAd.value != null || isNativeLoading ) {
            return
        }

        val loader = NativeAdLoader(context.applicationContext)

        val config = NativeAdRequestConfiguration.Builder(id)
                .build()

        loader.setNativeAdLoadListener(object : NativeAdLoadListener {
            override fun onAdLoaded(nativeAd: NativeAd) {
                DoraLogger.logYandexLoadSuccess(AdType.Native,id)
                _mNativeAd.update { nativeAd }
                isNativeLoading = false
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                DoraLogger.logYandexLoadFail(AdType.Native, id, error)
                _mNativeAd.update { null }
                isNativeLoading = false
            }
        })
        isNativeLoading = true
        loader.loadAd(config)
    }

    fun showNativeAd(
        viewGroup: ViewGroup,
        inflater: LayoutInflater,
        yandexNativeLayout: YandexNativeLayout?,
        callback: LoadNativeCallback?
    ) {
        if(_mNativeAd.value == null) {
            callback?.loadFailed()
        } else {
            val layout = when(yandexNativeLayout) {
                YandexNativeLayout.Native50 -> R.layout.dora_yandex_native_ad_50
                else -> R.layout.dora_yandex_native_ad_250
            }
            _mNativeAd.value?.let { nativeAd ->
                val itemView = inflater.inflate(
                    layout,
                    viewGroup,
                    false
                ) as NativeAdView

                val binder = NativeAdViewBinder.Builder(itemView)
                    .setCallToActionView(itemView.findViewById(R.id.yandex_ad_call_to_action))
                    .setDomainView(itemView.findViewById(R.id.yandex_ad_domain))
                    .setIconView(itemView.findViewById(R.id.yandex_ad_icon))
                    .setMediaView(itemView.findViewById(R.id.yandex_ad_media))
                    .setTitleView(itemView.findViewById(R.id.yandex_ad_title))
                    .setSponsoredView(itemView.findViewById(R.id.yandex_ad_sponsored))
                    .setFeedbackView(itemView.findViewById(R.id.yandex_ad_feedback))
                    .setWarningView(itemView.findViewById(R.id.yandex_ad_warning))
                    .build()

                try {
                    nativeAd.bindNativeAd(binder)

                    viewGroup.removeAllViews()
                    viewGroup.addView(itemView)
                    viewGroup.visibility = View.VISIBLE

                    _mNativeAd.update { null }
                    isNativeLoading = false

                } catch (e: Exception) {
                    Log.e("Dora", "Failed to bind fixed size native ad $e")
                }
            }
            callback?.loadSuccess()
        }

    }
}