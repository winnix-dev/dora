package com.winnix.dora.manager

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.winnix.dora.Dora
import com.winnix.dora.admob_manager.AdmobNative
import com.winnix.dora.admob_manager.NativeLayout
import com.winnix.dora.callback.LoadNativeCallback
import com.winnix.dora.model.NativeResult
import com.winnix.dora.model.NativeType
import com.winnix.dora.yandex_manager.YandexNativeLayout
import com.winnix.dora.yandex_manager.YandexNativeManger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal object NativeManager {
    fun loadAd(
        context: Context,
        id: String,
        yandexId: String?,
        nativeType: NativeType
    ){
        AdmobNative.loadAd(
            context = context,
            id = id,
            nativeType = nativeType
        )
        yandexId?.let {
            YandexNativeManger.loadNativeAd(context, it)
        }
    }

    fun loadAndShowAd(
        activity: Activity,
        id: String,
        yandexId: String?,
        lifecycleOwner: LifecycleOwner,
        nativeType: NativeType,
        layoutAd: NativeLayout,
        viewGroup: ViewGroup,
        callback: LoadNativeCallback? = null
    ) {
        loadAd(activity, id, yandexId, nativeType)
        if (Dora.canRequestAdmob(activity)) {
            lifecycleOwner.lifecycleScope.launch {
                AdmobNative.getAdState(nativeType).collectLatest { result ->
                    when(result) {
                        is NativeResult.Success -> {
                            AdmobNative.showNativeAd(
                                activity = activity,
                                nativeAd = result.ad,
                                viewLifecycle = lifecycleOwner.lifecycle,
                                layoutAd = layoutAd.getLayoutAd(),
                                viewGroup = viewGroup
                            )
                            AdmobNative.clearAd(NativeType.NATIVE)
                        }
                        is NativeResult.Failed -> {
                            YandexNativeManger.showNativeAd(
                                viewGroup = viewGroup,
                                inflater = activity.layoutInflater,
                                yandexNativeLayout = when(layoutAd) {
                                    NativeLayout.Native50, NativeLayout.NativeCollapsible -> {
                                        YandexNativeLayout.Native50
                                    }
                                    else -> YandexNativeLayout.Native250
                                }
                            )
                        }
                        else -> { }
                    }
                }
            }
        } else {
            YandexNativeManger.showNativeAd(
                viewGroup = viewGroup,
                inflater = activity.layoutInflater,
                yandexNativeLayout = when(layoutAd) {
                    NativeLayout.Native50, NativeLayout.NativeCollapsible -> {
                        YandexNativeLayout.Native50
                    }
                    else -> YandexNativeLayout.Native250
                }
            )
        }
    }

    fun clearNative(nativeType: NativeType) {
        AdmobNative.clearAd(nativeType)
    }

    fun getAdFullState() = AdmobNative.getAdState(NativeType.NATIVE_FULL)

}
