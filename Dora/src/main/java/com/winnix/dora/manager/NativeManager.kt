package com.winnix.dora.manager

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.winnix.dora.admob_manager.AdmobNative
import com.winnix.dora.admob_manager.NativeLayout
import com.winnix.dora.callback.LoadNativeCallback
import com.winnix.dora.model.NativeResult
import com.winnix.dora.model.NativeType
import com.winnix.dora.yandex_manager.YandexNativeLayout
import com.winnix.dora.yandex_manager.YandexNativeManger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

internal object NativeManager {
    private lateinit var admobId: String
    var yandexId: String? = null

    fun loadAd(
        context: Context,
        id: String,
        nativeType: NativeType
    ){
        admobId = id

        AdmobNative.loadAd(
            context = context,
            id = admobId,
            nativeType = nativeType
        )
        yandexId?.let {
            YandexNativeManger.loadNativeAd(context, it)
        }
    }

    fun loadAndShowAd(
        activity: Activity,
        id: String,
        lifecycleOwner: LifecycleOwner,
        nativeType: NativeType,
        layoutAd: NativeLayout,
        viewGroup: ViewGroup,
        callback: LoadNativeCallback? = null
    ) {
        admobId = id
        AdmobNative.resetState(nativeType)

        loadNativeInternal(activity, nativeType)

        lifecycleOwner.lifecycleScope.launch {
            merge(
                AdmobNative.getAdState(nativeType).filter { it is NativeResult.Success },
                YandexNativeManger.nativeAdFlow.filter { it != null }
                    .onStart {
                        delay(2000)
                    }
            )
                .first()

            val result = AdmobNative.nativeState.value[nativeType]
            if (result is NativeResult.Success) {
                AdmobNative.showNativeAd(
                    activity = activity,
                    nativeAd = result.ad,
                    viewLifecycle = lifecycleOwner.lifecycle,
                    layoutAd = layoutAd.getLayoutAd(),
                    viewGroup = viewGroup
                )
                AdmobNative.clearAd(NativeType.NATIVE)

                callback?.loadSuccess()
            }
            else {
                YandexNativeManger.showNativeAd(
                    viewGroup = viewGroup,
                    inflater = activity.layoutInflater,
                    yandexNativeLayout = when(layoutAd) {
                        NativeLayout.Native50, NativeLayout.NativeCollapsible -> {
                            YandexNativeLayout.Native50
                        }
                        else -> YandexNativeLayout.Native250
                    },
                    callback
                )
            }
        }
    }

    private fun loadNativeInternal(
        context: Context,
        nativeType: NativeType
    ) {
        AdmobNative.loadAd(
            context = context,
            id = admobId,
            nativeType = nativeType
        )

        yandexId?.let {
            YandexNativeManger.loadNativeAd(context, it)
        }
    }

    fun clearNative(nativeType: NativeType) {
        AdmobNative.clearAd(nativeType)
    }

    fun getAdFullState() = AdmobNative.getAdState(NativeType.NATIVE_FULL)

}
