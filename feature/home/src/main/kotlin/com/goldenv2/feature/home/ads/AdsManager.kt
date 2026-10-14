package com.goldenv2.feature.home.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

/**
 * Picks the ad network to use and forwards ad requests to it.
 *
 * AdMob (Google Mobile Ads) is preferred. It is skipped and [YandexAdProvider]
 * takes over when AdMob is not configured, when the device has no Google Play
 * Services, or when AdMob repeatedly fails to fill. When no network is usable
 * the app falls back to [NoAdsProvider] and simply shows no ads.
 *
 * The VPN connection itself is never blocked on an ad: if no ad is ready, or
 * loading/showing fails, [showAdOrProceed] invokes `onProceed` immediately so
 * the connect flow continues uninterrupted.
 */
@Singleton
class AdsManager internal constructor(
    /** Reads the persisted consent at the moment a network is selected. */
    private val isPersonalizationAllowed: () -> Boolean,
    /** Ad networks in fallback order. */
    private val providers: List<AdProvider>
) {

    constructor(
        consentManager: ConsentManager,
        providers: List<AdProvider>
    ) : this({ consentManager.isPersonalizationAllowed }, providers)

    private val _activeProvider = MutableStateFlow<AdProvider>(NoAdsProvider)

    /** Network currently serving ads, or [NoAdsProvider] while none is ready. */
    val activeProvider: StateFlow<AdProvider> = _activeProvider.asStateFlow()

    /** Index into [providers] of the network being initialized, or -1 for none. */
    @Volatile
    private var activeIndex = -1

    private val started = AtomicBoolean(false)
    private val consecutiveLoadFailures = AtomicInteger(0)

    /** Initializes the preferred available network. Safe to call more than once. */
    fun initialize(context: Context) {
        if (!started.compareAndSet(false, true)) return
        activate(context.applicationContext, 0)
    }

    /**
     * Selects the first available network at or after [from] and initializes it.
     * Availability is re-checked on every attempt, so a network demoted mid-session
     * cannot be re-selected.
     */
    private fun activate(context: Context, from: Int) {
        val provider = providers.getOrNull(from)
        if (provider == null) {
            Log.w(TAG, "No ad network available")
            _activeProvider.value = NoAdsProvider
            return
        }
        if (!provider.isAvailable(context)) {
            Log.i(TAG, "${provider.network} unavailable, falling back")
            activate(context, from + 1)
            return
        }
        activeIndex = from
        provider.setPersonalized(isPersonalizationAllowed())
        provider.initialize(
            context = context,
            onReady = {
                Log.i(TAG, "Ad network active: ${provider.network}")
                consecutiveLoadFailures.set(0)
                _activeProvider.value = provider
            },
            onLoadFailed = { onLoadFailed(context) }
        )
    }

    /**
     * Counts load failures for the active network and demotes it to the next
     * fallback once it stops filling, so the app keeps earning revenue.
     */
    private fun onLoadFailed(context: Context) {
        val failures = consecutiveLoadFailures.incrementAndGet()
        if (failures < MAX_CONSECUTIVE_LOAD_FAILURES) {
            Log.i(TAG, "Ad load failed ($failures/$MAX_CONSECUTIVE_LOAD_FAILURES)")
            return
        }
        val next = activeIndex + 1
        if (next >= providers.size) {
            Log.w(TAG, "No fallback ad network left, keeping current provider")
            consecutiveLoadFailures.set(0)
            return
        }
        Log.w(TAG, "Ad network stopped filling, falling back")
        consecutiveLoadFailures.set(0)
        _activeProvider.value = NoAdsProvider
        activate(context, next)
    }

    /**
     * Shows the preloaded interstitial, then calls [onProceed] once the ad is
     * dismissed (or immediately if no ad is available / showing fails).
     * Must be called from the main thread with a foreground [activity].
     */
    fun showAdOrProceed(activity: Activity, onProceed: () -> Unit) {
        _activeProvider.value.showInterstitial(activity, onProceed)
    }

    /**
     * Re-applies consent after the user changed their answer. Forwarded to every
     * provider so a network selected later starts from the current answer.
     */
    fun onConsentChanged(granted: Boolean) {
        providers.forEach { it.setPersonalized(granted) }
    }

    private companion object {
        private const val TAG = "AdsManager"

        /** Consecutive load failures before falling back to the next network. */
        private const val MAX_CONSECUTIVE_LOAD_FAILURES = 3
    }
}