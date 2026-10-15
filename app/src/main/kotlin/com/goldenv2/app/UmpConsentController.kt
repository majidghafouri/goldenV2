package com.goldenv2.app

import android.app.Activity
import android.content.Context
import android.util.Log
import com.goldenv2.feature.home.ads.AdsManager
import com.goldenv2.feature.home.ads.ConsentManager
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives Google's User Messaging Platform (UMP) consent flow and feeds the result
 * into [ConsentManager].
 *
 * UMP owns the legally-required consent record for EEA, UK and Swiss users. It is
 * IAB TCF certified, and Google's EU User Consent Policy requires it for those
 * regions; without it ad serving there is restricted.
 *
 * The app's own "Data & Ads Consent" dialog still covers everyone else and still
 * gates AppMetrica. Both answers are combined with AND in [ConsentManager] so the
 * app can only ever be more restrictive than either source, never less.
 *
 * The consent message itself is created and published in the AdMob console under
 * Privacy & messaging > European regulations. The SDK finds it via the AdMob
 * application ID already declared in the manifest, so there is no tag ID to
 * configure here.
 */
@Singleton
class UmpConsentController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentManager: ConsentManager,
    private val adsManager: AdsManager
) {

    /**
     * Requests the latest consent status and, when a form is required or the user
     * has not decided yet, shows it. Must be called from the main thread with a
     * resumed [activity].
     */
    fun collectConsentIfRequired(activity: Activity, onComplete: () -> Unit = {}) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        val parameters = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            parameters,
            { // Status is current; show the form if it is still outstanding.
                if (consentInformation.isConsentFormAvailable) {
                    loadAndShow(consentInformation, activity, onComplete)
                } else {
                    applyConsent(consentInformation)
                    onComplete()
                }
            },
            { error ->
                // A failed status fetch must not leave the app in a state where ads
                // stay personalized on the user's previous answer alone.
                Log.w(TAG, "Consent info update failed: ${error.message}")
                consentManager.setUmpAllowsAds(false)
                adsManager.onConsentChanged(consentManager.isPersonalizationAllowed)
                onComplete()
            }
        )
    }

    private fun loadAndShow(
        consentInformation: ConsentInformation,
        activity: Activity,
        onComplete: () -> Unit
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
            activity,
            ConsentForm.OnConsentFormDismissedListener { error ->
                if (error != null) {
                    // The user dismissed the form without deciding, which counts as
                    // withholding consent.
                    Log.w(TAG, "Consent form dismissed with error: ${error.message}")
                    consentManager.setUmpAllowsAds(false)
                }
                applyConsent(consentInformation)
                onComplete()
            }
        )
    }

    /**
     * Re-opens the CMP so the user can change an earlier decision. GDPR requires
     * consent to be withdrawable at any time.
     */
    fun showPrivacyOptions(activity: Activity, onComplete: () -> Unit = {}) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        UserMessagingPlatform.showPrivacyOptionsForm(
            activity,
            ConsentForm.OnConsentFormDismissedListener {
                applyConsent(consentInformation)
                onComplete()
            }
        )
    }

    private fun applyConsent(consentInformation: ConsentInformation) {
        val allowed = consentInformation.canRequestAds()
        Log.d(TAG, "UMP status=${consentInformation.consentStatus} canRequestAds=$allowed")
        consentManager.setUmpAllowsAds(allowed)
        adsManager.onConsentChanged(consentManager.isPersonalizationAllowed)
    }

    private companion object {
        private const val TAG = "UmpConsentController"
    }
}