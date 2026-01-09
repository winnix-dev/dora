package com.winnix.dora.manager

import android.app.Activity
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.winnix.dora.Dora
import com.winnix.dora.admob_manager.AdmobBanner
import com.winnix.dora.callback.LoadBannerCallback
import com.winnix.dora.model.AdmobBannerSize
import com.winnix.dora.yandex_manager.YandexBannerManager

object BannerManager {
    fun loadBanner(
        activity: Activity,
        container: ViewGroup,
        adSize: AdmobBannerSize,
        lifecycleOwner: LifecycleOwner,
        admobId: String,
        yandexId: String?,
    ) {
        AdmobBanner.loadBanner(
            id = admobId,
            activity = activity,
            container = container,
            adSize = adSize,
            lifecycleOwner = lifecycleOwner,
            callback = object : LoadBannerCallback {
                override fun onLoadFailed() {
                    yandexId?.let {
                        YandexBannerManager.loadBanner(
                            activity = activity,
                            container = container,
                            lifecycleOwner = lifecycleOwner,
                            id = it,
                            callback = object : LoadBannerCallback {},
                        )
                    }
                }
            }
        )
    }
}