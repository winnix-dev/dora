package com.winnix.dora.helper

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

internal object UMPHelper {
    private fun getConsentInfo(activity: Activity) : ConsentInformation {
        return UserMessagingPlatform.getConsentInformation(activity)
    }
    fun fetchConsent(activity: Activity, debugId: String?, onComplete: (Boolean) -> Unit) {
        activity.runOnUiThread {

            val paramsBuilder = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)

            if(debugId != null) {
                val debugSettings = ConsentDebugSettings.Builder(activity)
                    .addTestDeviceHashedId(debugId)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build()

                paramsBuilder.setConsentDebugSettings(debugSettings)
            }

            val params = paramsBuilder.build()
            val consentInformation = getConsentInfo(activity)

            consentInformation.requestConsentInfoUpdate(
                activity, params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                        if (loadAndShowError != null) {
                            Log.e("Dora", "Failed to show form: $loadAndShowError")
                        }

                        if (canRequestAds(activity)) {
                            onComplete(true)
                        } else {
                            Log.e("Dora", "Consent gathered but cannot request ads (User refused or error)")
                            onComplete(false)
                        }
                    }
                },
                { requestConsentError  ->
                    Log.e("Dora", "fetchConsent $requestConsentError ")

                    onComplete(consentInformation.canRequestAds())
                }
            )
        }
    }

    fun canRequestAds(activity: Activity): Boolean {
        return getConsentInfo(activity).canRequestAds()
    }
    fun isPrivacyOptionsRequired(activity: Activity): Boolean {
        return getConsentInfo(activity).privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismiss: (Boolean) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.e("Dora", "Privacy Form Error: ${formError.errorCode} - ${formError.message} " )
                onDismiss(false)
            } else {
                onDismiss(true)
            }
        }
    }
}