package com.goldenv2.feature.home.ads

import android.content.Context
import com.yandex.mobile.ads.common.YandexAds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the user's data-processing consent and propagates it to the
 * Yandex Mobile Ads SDK. AppMetrica is updated by [com.goldenv2.app.GoldenV2Application],
 * which collects [personalizationConsent] and calls
 * `AppMetrica.setDataSendingEnabled` accordingly.
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

    /** `null` until the user has answered the consent dialog. */
    val personalizationConsent: StateFlow<Boolean?> = _personalizationConsent

    val isPersonalizationAllowed: Boolean
        get() = _personalizationConsent.value == true

    fun setPersonalizationConsent(granted: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT, granted).apply()
        _personalizationConsent.value = granted
        YandexAds.setUserConsent(granted)
    }

    private companion object {
        private const val PREFS_NAME = "ads_consent"
        private const val KEY_CONSENT = "personalization_consent"
    }
}
