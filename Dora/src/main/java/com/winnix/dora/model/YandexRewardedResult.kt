package com.winnix.dora.model

import com.yandex.mobile.ads.rewarded.RewardedAd


sealed class YandexRewardedResult {
    object Idle : YandexRewardedResult()
    object Loading : YandexRewardedResult()
    object Failed: YandexRewardedResult()
    data class Success(val ad: RewardedAd) : YandexRewardedResult()
}