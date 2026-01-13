package com.winnix.dora.manager

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.winnix.dora.admob_manager.AdmobRewarded
import com.winnix.dora.callback.LoadRewardedCallback
import com.winnix.dora.callback.ShowRewardedCallback
import com.winnix.dora.model.RewardedResult
import com.winnix.dora.model.YandexRewardedResult
import com.winnix.dora.yandex_manager.YandexRewardedManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object RewardManager {
    var yandexId: String? = null


    fun isAdmobAvailable() = AdmobRewarded.rewardAd.value is RewardedResult.Success

    fun loadAd(
        context: Context,
        id: String,
        callback: LoadRewardedCallback?
    ) {

        AdmobRewarded.loadAd(
            context, id, callback
        )
        yandexId?.let {
            YandexRewardedManager.loadAd(context, id)
        }
    }

    fun showAd(
        activity: AppCompatActivity,
        timeoutLong: Long,
        callback: ShowRewardedCallback
    ) {
        activity.lifecycleScope.launch {
            withTimeoutOrNull(timeoutLong) {
                AdmobRewarded.rewardAd.first { it is RewardedResult.Success || it is RewardedResult.Failed }
                if(AdmobRewarded.rewardAd.value !is RewardedResult.Success) {
                    YandexRewardedManager.adState.first{ it is YandexRewardedResult.Success || it is YandexRewardedResult.Failed}
                }
            }

            AdmobRewarded.showAd(
                activity,
                object : ShowRewardedCallback {
                    override fun showSuccess() {
                        callback.showSuccess()
                    }

                    override fun onShow() {
                        callback.onShow()
                    }

                    override fun showFailed() {
                        YandexRewardedManager.showAd(
                            activity,
                            object : ShowRewardedCallback {
                                override fun showFailed() {
                                    callback.showFailed()
                                }

                                override fun showSuccess() {
                                    callback.showSuccess()
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}