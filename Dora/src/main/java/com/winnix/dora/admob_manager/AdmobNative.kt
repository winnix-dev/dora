package com.winnix.dora.admob_manager

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.winnix.dora.Dora
import com.winnix.dora.admob_manager.NativeHelper.registerWithLifecycle
import com.winnix.dora.model.NativeResult
import com.winnix.dora.model.NativeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object AdmobNative {
    private val _nativeState = MutableStateFlow<Map<NativeType, NativeResult>>(mapOf())

    fun getAdState(nativeType: NativeType): Flow<NativeResult> =
        _nativeState.map { map ->
            map.getOrElse(nativeType) { NativeResult.Idle }
        }

    fun loadAd(
        context: Context,
        id: String,
        nativeType: NativeType
    ) {
        val state = _nativeState.value.getOrElse(nativeType) { NativeResult.Idle }
        if (state is NativeResult.Loading || state is NativeResult.Success) {
            return
        }

        updateAd(nativeType, NativeResult.Loading)

        CoroutineScope(Dispatchers.IO).launch {
            Dora.ensureInitialized()

            val adLoader = AdLoader.Builder(context.applicationContext, id)
                .forNativeAd { ad ->
                    updateAd(nativeType, NativeResult.Success(ad))
                }
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setVideoOptions(
                            VideoOptions.Builder()
                                .setStartMuted(true)
                                .build()
                        )
                        .build()
                )
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(p0: LoadAdError) {
                            super.onAdFailedToLoad(p0)
                            Log.e("Dora", "Load Admob Failed $p0")

                            updateAd(nativeType, NativeResult.Failed)
                        }
                    }
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    fun showNativeAd(
        activity: Activity,
        nativeAd: NativeAd,
        viewLifecycle: Lifecycle,
        layoutAd: Int,
        viewGroup: ViewGroup,
    ) {
        val layout = LayoutInflater.from(activity).inflate(layoutAd, null)

        activity.runOnUiThread {
            NativeHelper.getNativeAdView(
                activity,
                nativeAd,
                layout
            ).let { adView ->
                nativeAd.registerWithLifecycle(viewLifecycle)
                adView.setNativeAd(nativeAd)
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
            }
        }
    }

    fun clearAd(adType: NativeType) {
        updateAd(adType, NativeResult.Idle)
    }

    private fun updateAd(nativeType: NativeType, state: NativeResult) {
        _nativeState.update {
            val map = _nativeState.value.toMutableMap()
            map[nativeType] = state
            map
        }
    }
}