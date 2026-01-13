package com.winnix.dora.model

import com.google.android.gms.ads.rewarded.RewardedAd

sealed class RewardedResult {
    object Idle : RewardedResult()
    object Loading : RewardedResult()
    object Failed: RewardedResult()
    data class Success(val ad: RewardedAd) : RewardedResult()
}