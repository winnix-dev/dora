package com.winnix.dora.admob_manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.winnix.dora.Dora.TAG
import com.winnix.dora.callback.LoadRewardedCallback
import com.winnix.dora.callback.ShowRewardedCallback
import com.winnix.dora.model.RewardedResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AdmobRewarded {
    private val _rewardedAd = MutableStateFlow<RewardedResult>(RewardedResult.Idle)
    val rewardAd = _rewardedAd.asStateFlow()


    fun loadAd(
        context: Context,
        id: String,
        listener: LoadRewardedCallback?
    ) {
        if (_rewardedAd.value is RewardedResult.Success || _rewardedAd.value is RewardedResult.Loading) {
            return
        }

        _rewardedAd.update { RewardedResult.Loading }

        RewardedAd.load(
            context.applicationContext,
            id,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(p0: RewardedAd) {
                    Log.d(TAG, "Admob Rewarded Loaded")
                    _rewardedAd.update { RewardedResult.Success(p0) }
                    listener?.onLoadSuccess()
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    Log.e(TAG, "Admob Rewarded LoadFail $p0")
                    _rewardedAd.update { RewardedResult.Failed }
                    listener?.onLoadFailed()
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        callback: ShowRewardedCallback
    ) {
        if (_rewardedAd.value is RewardedResult.Success) {
            val rewardedAd = (_rewardedAd.value as RewardedResult.Success).ad
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded was dismissed.")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Rewarded failed to show.")
                    _rewardedAd.update { RewardedResult.Idle }
                    callback.showFailed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded showed fullscreen content.")
                    _rewardedAd.update { RewardedResult.Idle }
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Rewarded recorded an impression.")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Rewarded was clicked.")
                }
            }
            rewardedAd.show(
                activity
            ) { rewardItem ->
                Log.d(TAG, "User earned the reward. $rewardItem")
                callback.showSuccess()
            }
        }
        else {
            callback.showFailed()
        }
    }
}