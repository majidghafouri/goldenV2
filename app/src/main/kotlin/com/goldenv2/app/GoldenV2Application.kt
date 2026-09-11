package com.goldenv2.app

import android.app.Application
import com.goldenv2.feature.home.ads.ConsentManager
import dagger.hilt.android.HiltAndroidApp
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import io.appmetrica.analytics.push.AppMetricaPush
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class GoldenV2Application : Application() {

    @Inject
    lateinit var consentManager: ConsentManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        initAppMetrica()
    }

    private fun initAppMetrica() {
        if (BuildConfig.APPMETRICA_API_KEY.isBlank() ||
            BuildConfig.APPMETRICA_API_KEY.equals("test", ignoreCase = true)
        ) {
            android.util.Log.w(TAG, "AppMetrica not activated: no real API key configured")
            return
        }
        val config = AppMetricaConfig.newConfigBuilder(BuildConfig.APPMETRICA_API_KEY)
            // Data is not sent until the user grants consent; updated below once
            // the user answers the consent dialog.
            .withDataSendingEnabled(consentManager.isPersonalizationAllowed)
            .withLogs()
            .build()
        AppMetrica.activate(this, config)
        AppMetrica.enableActivityAutoTracking(this)
        // Yandex Mobile Ads SDK auto-reports ad revenue to AppMetrica once activated.
        activatePushIfFirebaseConfigured()

        appScope.launch {
            consentManager.personalizationConsent.collect { granted ->
                AppMetrica.setDataSendingEnabled(granted == true)
            }
        }
    }

    /**
     * AppMetricaPush's default activate() eagerly creates a Firebase push
     * controller, which throws IllegalStateException when the app has no
     * Firebase project (google-services.json). Skip push entirely in that case.
     */
    private fun activatePushIfFirebaseConfigured() {
        val googleAppId = resources.getIdentifier("google_app_id", "string", packageName)
        if (googleAppId == 0) {
            android.util.Log.i(TAG, "AppMetrica Push skipped: no Firebase project configured")
            return
        }
        try {
            AppMetricaPush.activate(applicationContext)
        } catch (e: IllegalStateException) {
            android.util.Log.w(TAG, "AppMetrica Push activation failed: ${e.message}")
        }
    }

    private companion object {
        private const val TAG = "GoldenV2App"
    }
}
