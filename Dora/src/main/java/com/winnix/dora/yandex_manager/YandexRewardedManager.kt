package com.winnix.dora.yandex_manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.winnix.dora.Dora.TAG
import com.winnix.dora.callback.ShowRewardedCallback
import com.winnix.dora.model.YandexRewardedResult
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object YandexRewardedManager {
    private val _adState = MutableStateFlow<YandexRewardedResult>(YandexRewardedResult.Idle)
    val adState = _adState.asStateFlow()


    fun loadAd(
        context: Context,
        id: String
    ) {
        if (_adState.value is YandexRewardedResult.Loading || _adState.value is YandexRewardedResult.Success) {
            return
        }

        val rewardedAdLoader = RewardedAdLoader(context.applicationContext).apply {
            setAdLoadListener(
                object : RewardedAdLoadListener {
                    override fun onAdFailedToLoad(error: AdRequestError) {
                        Log.e(TAG, "Yandex Rewarded LoadFail $error")
                        _adState.update { YandexRewardedResult.Failed }
                    }

                    override fun onAdLoaded(rewarded: RewardedAd) {
                        Log.d(TAG, "Yandex Rewarded Loaded")
                        _adState.update { YandexRewardedResult.Success(rewarded) }
                    }
                }
            )
        }
        val adRequestConfiguration = AdRequestConfiguration.Builder(id).build()

        _adState.update { YandexRewardedResult.Loading }

        rewardedAdLoader.loadAd(adRequestConfiguration)
    }

    fun showAd(
        activity: Activity,
        callback: ShowRewardedCallback
    ) {
        val rewardedAd = _adState.value

        if (rewardedAd is YandexRewardedResult.Success) {
            rewardedAd.ad.apply {
                setAdEventListener(object : RewardedAdEventListener {
                    override fun onAdClicked() {
                        Log.d(TAG, "YandexRewarded onAdClicked")
                    }

                    override fun onAdDismissed() {
                        Log.d(TAG, "YandexRewarded Dismissed")
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        Log.e(TAG, "YandexRewarded ShowFail $adError")
                        _adState.update { YandexRewardedResult.Idle }
                        callback.showFailed()
                    }

                    override fun onAdImpression(impressionData: ImpressionData?) {
                        Log.d(TAG, "YandexRewarded Impression")
                    }

                    override fun onAdShown() {
                        Log.d(TAG, "YandexRewarded Show")
                        _adState.update { YandexRewardedResult.Idle }
                        callback.onShow()
                    }

                    override fun onRewarded(reward: Reward) {
                        Log.d(TAG, "YandexRewarded onRewarded: $reward")
                        callback.showSuccess()
                    }

                })
                show(activity)
            }
        } else {
            callback.showFailed()
        }
    }
}