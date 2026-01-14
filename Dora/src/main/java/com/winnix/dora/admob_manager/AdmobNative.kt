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
import com.winnix.dora.helper.DoraLogger
import com.winnix.dora.model.AdType
import com.winnix.dora.model.NativeResult
import com.winnix.dora.model.NativeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

internal object AdmobNative {
    private val _nativeState = MutableStateFlow<Map<NativeType, NativeResult>>(mapOf())
    val nativeState = _nativeState.asStateFlow()

    private val nativeWaitingState : ConcurrentHashMap<NativeType, Boolean> = ConcurrentHashMap()
    private val nativeWaitingJob: ConcurrentHashMap<NativeType, Job> = ConcurrentHashMap()
    private val nativeRetryTimes: ConcurrentHashMap<NativeType, Int> = ConcurrentHashMap()
    private val nativeBackoffTime = mutableMapOf(
        NativeType.NATIVE to 3000L,
        NativeType.NATIVE_FULL to 5000L,
    )
    private val nativeBackoffMaxTime = mutableMapOf(
        NativeType.NATIVE to 4,
        NativeType.NATIVE_FULL to 3,
    )

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
        val isWaiting = nativeWaitingState.getOrElse(nativeType) { false }

        if (state is NativeResult.Loading || state is NativeResult.Success || isWaiting) {
            return
        }

        updateAd(nativeType, NativeResult.Loading)

        CoroutineScope(Dispatchers.IO).launch {
            Dora.ensureInitialized()

            val adLoader = AdLoader.Builder(context.applicationContext, id)
                .forNativeAd { ad ->
                    DoraLogger.logAdMobLoadSuccess(AdType.Native, id)
                    resetState(nativeType)
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
                            DoraLogger.logAdMobLoadFail(AdType.Native,id, p0)

                            handleLoadAdFail(nativeType, context, id)
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()

                            Log.d("Dora", "Native Impression $id")
                        }
                    }
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    private fun handleLoadAdFail(
        nativeType: NativeType,
        context: Context,
        id: String
    ) {
        updateAd(nativeType, NativeResult.Failed)

        nativeWaitingState[nativeType] = true
        val currentRetryTimes = nativeRetryTimes.getOrElse(nativeType) { 0 }
        nativeRetryTimes[nativeType] = currentRetryTimes + 1
        val backoffTime = nativeBackoffTime.getOrElse(nativeType) { 3000L }
        val maxRetryTimes = nativeBackoffMaxTime.getOrElse(nativeType) { 3 }

        val delayTime = backoffTime * (currentRetryTimes + 1).coerceAtMost(maxRetryTimes)

        nativeWaitingJob[nativeType] = CoroutineScope(Dispatchers.Default).launch {
            delay(delayTime)
            nativeWaitingState[nativeType] = false

            loadAd(
                context, id, nativeType
            )
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
        resetState(adType)
        updateAd(adType, NativeResult.Idle)
    }

    fun resetState(adType: NativeType) {
        nativeWaitingState[adType] = false
        nativeRetryTimes[adType] = 0
        nativeWaitingJob[adType]?.cancel()
    }

    private fun updateAd(nativeType: NativeType, state: NativeResult) {
        _nativeState.update {
            val map = _nativeState.value.toMutableMap()
            map[nativeType] = state
            map
        }
    }
}