package com.winnix.dora.helper

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
import com.winnix.dora.helper.NativeHelper.registerWithLifecycle
import com.winnix.dora.model.AdUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NativeManager {
    // Config
    private var maxAdCache = 2
    private var intervalTime = 3000L

    private var listAds = listOf<AdUnit>(
//        AdmobResource.NATIVE_ALL,
//        AdmobResource.SUCCESS_NATIVE,
    )

    // State
    private var isLoading = false
    private var currentIndex = 0
    private val _adState = MutableStateFlow(ArrayDeque<NativeAd>())
    val adState =  _adState.asStateFlow()

    fun configAd(
        listAds: List<AdUnit>,
        maxAdCache: Int = 2,
        intervalTime: Long = 3000L,
    ) {
        this.listAds = listAds
        this.maxAdCache = maxAdCache
        this.intervalTime = intervalTime
    }

    fun loadAd(
        context: Context,
    ) {
        if(listAds.isEmpty()) {
            Log.e("TAG", "Native Ads is Empty!")
            return
        }

        if(isLoading || _adState.value.size >= maxAdCache) {
            return
        }

        isLoading = true

        CoroutineScope(Dispatchers.IO).launch {
            val adLoader =
                AdLoader.Builder(
                    context.applicationContext,
                    Dora.getAdmobId(listAds[currentIndex % listAds.size])
                )
                    .forNativeAd { ad ->

                        _adState.update {
                            val deque = ArrayDeque(it)
                            deque.addLast(ad)
                            deque
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            isLoading = false
                            delay(intervalTime)
                            loadAd(context)
                        }
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

                                Log.e("Dora", "Load Native Fail: $p0" )

                                currentIndex++

                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(intervalTime)
                                    isLoading = false
                                    loadAd(context)
                                }
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

    fun getAndPop(context: Context) : NativeAd? {
        val ad = _adState.value.firstOrNull()

        if(ad != null) {
            _adState.update {
                val deque = ArrayDeque(it)
                deque.removeFirst()
                deque
            }

            CoroutineScope(Dispatchers.IO).launch {
                loadAd(context)
            }
        }

        return ad
    }

}