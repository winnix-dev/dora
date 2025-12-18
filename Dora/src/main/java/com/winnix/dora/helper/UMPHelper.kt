package com.winnix.dora.helper

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

internal object UMPHelper {
    fun fetchConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        activity.runOnUiThread {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            val paramsBuilder = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)

//            if(BuildConfig.DEBUG) {
//                val debugSettings = ConsentDebugSettings.Builder(activity)
//                    .addTestDeviceHashedId("B3EEABB8EE11C2BE770B684D95219ECB")
//                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
//                    .build()
//
//                paramsBuilder.setConsentDebugSettings(debugSettings)
//            }

            val params = paramsBuilder.build()

            consentInformation.requestConsentInfoUpdate(
                activity, params,
                {
                    val isConsentFormAvailable = consentInformation.isConsentFormAvailable

                    if (isConsentFormAvailable) {
                        loadAndShowFormManual(
                            activity,
                            onComplete
                        )
                    } else {
                        onComplete(consentInformation.canRequestAds())
                    }
                },
                {
                    onComplete(consentInformation.canRequestAds())
                }
            )
        }
    }
    private fun loadAndShowFormManual(activity: Activity, onComplete: (Boolean) -> Unit) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { consentForm ->
                if(!activity.isFinishing && !activity.isDestroyed) {
                    consentForm.show(activity) {
                        val canRequest = UserMessagingPlatform.getConsentInformation(activity).canRequestAds()
                        onComplete(canRequest)
                    }
                } else {
                    onComplete(false)
                }
            },
            {
                val canRequest = UserMessagingPlatform.getConsentInformation(activity).canRequestAds()
                onComplete(canRequest)
            }
        )
    }

    fun isPrivacyOptionsRequired(activity: Activity) : Boolean {
        return UserMessagingPlatform.getConsentInformation(activity)
            .privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptionsForm(activity: Activity) : Boolean {
        var isSuccess = true
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            isSuccess = formError != null
        }

        return isSuccess
    }
}