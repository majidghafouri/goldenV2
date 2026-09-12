package com.goldenv2.feature.home.ads

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether the interstitial ad should be shown on a connect press.
 *
 * - The first [GRACE_USAGE_COUNT] connect presses never show an ad.
 * - Afterwards, the ad is shown once every [SHOW_EVERY_N_PRESSES] presses.
 *
 * The press counter is persisted in SharedPreferences so it survives
 * process restarts and app updates.
 */
@Singleton
class AdFrequencyManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Registers a Connect press and reports whether an ad should be shown. */
    fun shouldShowAdOnConnect(): Boolean {
        val presses = prefs.getLong(KEY_CONNECT_PRESSES, 0L) + 1
        prefs.edit().putLong(KEY_CONNECT_PRESSES, presses).apply()

        return presses > GRACE_USAGE_COUNT &&
            (presses - GRACE_USAGE_COUNT) % SHOW_EVERY_N_PRESSES == 0L
    }

    private companion object {
        private const val PREFS_NAME = "ads_frequency"
        private const val KEY_CONNECT_PRESSES = "connect_presses"
        private const val GRACE_USAGE_COUNT = 50L
        private const val SHOW_EVERY_N_PRESSES = 3L
    }
}
