package com.winnix.dora.helper

import android.util.Log
import com.google.android.gms.ads.LoadAdError
import com.winnix.dora.model.AdType
import com.google.android.gms.ads.AdError as AdMobError
import com.yandex.mobile.ads.common.AdError as YandexShowError
import com.yandex.mobile.ads.common.AdRequestError as YandexRequestError

object DoraLogger {
    private const val TAG = "Dora"

    // Generic Logger for custom messages
    fun log(message: String) {
        Log.d(TAG, message)
    }

    fun logAdMobLoadFail(adType: AdType, adUnitId: String, error: LoadAdError) {
        val errorMsg = """
            ❌ ADMOB LOAD FAILED
            --------------------------------------------------
            📦 Type       : ${adType::class.simpleName}
            🆔 Unit ID    : $adUnitId
            ⚠️ Code       : ${error.code}
            💬 Message    : ${error.message}
            ℹ️ Domain     : ${error.domain}
            --------------------------------------------------
        """.trimIndent()
        Log.e(TAG, errorMsg)
    }

    /**
     * Logs AdMob Show Failures
     */
    fun logAdMobShowFail(adType: AdType, error: AdMobError) {
        val causeString = error.cause?.let {
            "\n            Caused by  : $it"
        } ?: ""
        val errorMsg = """
            🚫 ADMOB SHOW FAILED
            --------------------------------------------------
            📦 Type       : ${adType::class.simpleName}
            ⚠️ Code       : ${error.code}
            💬 Message    : ${error.message}
            ℹ️ Domain     : ${error.domain}$causeString
            --------------------------------------------------
        """.trimIndent()
        Log.e(TAG, errorMsg)
    }

    /**
     * Logs Yandex Load Failures
     */
    fun logYandexLoadFail(adType: AdType, adUnitId: String, error: YandexRequestError) {
        val errorMsg = """
            ❌ YANDEX LOAD FAILED
            --------------------------------------------------
            📦 Type       : ${adType::class.simpleName}
            🆔 Unit ID    : $adUnitId
            ⚠️ Code       : ${error.code}
            💬 Description: ${error.description}
            --------------------------------------------------
        """.trimIndent()
        Log.e(TAG, errorMsg)
    }

    /**
     * Logs Yandex Show Failures
     */
    fun logYandexShowFail(adType: AdType, error: YandexShowError) {
        val errorMsg = """
            🚫 YANDEX SHOW FAILED
            --------------------------------------------------
            📦 Type       : ${adType::class.simpleName}
            💬 Description: ${error.description}
            --------------------------------------------------
        """.trimIndent()
        Log.e(TAG, errorMsg)
    }

    /**
     * Logs AdMob Load Success
     */
    fun logAdMobLoadSuccess(adType: AdType, adUnitId: String) {
        val msg = """
            ✅ ADMOB LOAD SUCCESS
            --------------------------------------------------
            📦 Type       : ${adType::class.simpleName}
            🆔 Unit ID    : $adUnitId
            --------------------------------------------------
        """.trimIndent()
        Log.i(TAG, msg)
    }

    /**
     * Logs Yandex Load Success
     */
    fun logYandexLoadSuccess(adType: AdType, adUnitId: String) {
        val msg = """
            ✅ YANDEX LOAD SUCCESS
            --------------------------------------------------
            📦 Type       : ${adType::class.simpleName}
            🆔 Unit ID    : $adUnitId
            --------------------------------------------------
        """.trimIndent()
        Log.i(TAG, msg)
    }
}