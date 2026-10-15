package com.goldenv2.feature.home.ads

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the user's data-processing consent and exposes it to the ad networks
 * through [AdsManager], which forwards it to the active [AdProvider].
 * AppMetrica is updated by [com.goldenv2.app.GoldenV2Application], which collects
 * [personalizationConsent] and calls `AppMetrica.setDataSendingEnabled` accordingly.
 *
 * This class is deliberately free of ad SDK types so it can be injected from
 * `:app` without pulling an ad SDK into the application module.
 *
 * Consent is stored synchronously (SharedPreferences) because it is needed in
 * `Application.onCreate()` before AppMetrica is activated.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _personalizationConsent = MutableStateFlow(
        if (prefs.contains(KEY_CONSENT)) prefs.getBoolean(KEY_CONSENT, false) else null
    )

    /**
     * Whether Google's User Messaging Platform currently allows ad requests.
     * Defaults to `true` so non-EEA users, where no CMP is required, are unaffected
     * and the first ad request is not needlessly downgraded while UMP is still loading.
     */
    private val _umpAllowsAds = MutableStateFlow(true)

    /** `null` until the user has answered the in-app consent dialog. */
    val personalizationConsent: StateFlow<Boolean?> = _personalizationConsent

    /** The in-app consent answer on its own, ignoring the Google CMP. */
    val isUserConsentGranted: Boolean
        get() = _personalizationConsent.value == true

    /**
     * Effective advertising consent: the in-app answer AND the Google CMP must both
     * allow it. Using AND keeps the two sources from ever disagreeing in the app's
     * favour - if either withholds consent, ads are requested non-personalized.
     */
    val isPersonalizationAllowed: Boolean
        get() = isUserConsentGranted && _umpAllowsAds.value

    fun setPersonalizationConsent(granted: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT, granted).apply()
        _personalizationConsent.value = granted
    }

    /**
     * Records whether the Google CMP currently permits ad requests. Called by
     * [com.goldenv2.app.UmpConsentController] once the UMP status is known.
     */
    fun setUmpAllowsAds(allowed: Boolean) {
        _umpAllowsAds.value = allowed
    }

    private companion object {
        private const val PREFS_NAME = "ads_consent"
        private const val KEY_CONSENT = "personalization_consent"
    }
}
