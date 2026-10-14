package com.goldenv2.feature.home.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.goldenv2.feature.home.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Mobile Ads (AdMob) implementation of [AdProvider].
 *
 * AdMob cannot fill without Google Play Services, so [isAvailable] reports the
 * network as unavailable on devices that have none (and when the ad unit IDs are
 * not configured). [AdsManager] then falls back to Yandex Mobile Ads.
 */
@Singleton
class AdmobAdProvider @Inject constructor() : AdProvider {

    override val network: AdNetwork = AdNetwork.ADMOB

    private var interstitialAd: InterstitialAd? = null
    private var onLoadFailedCallback: () -> Unit = {}
    private val loading = AtomicBoolean(false)

    /** Read during composition so a consent change re-creates the banner. */
    private val personalized = mutableStateOf(false)

    @Volatile
    private var sdkInitialized = false

    override fun isAvailable(context: Context): Boolean {
        if (!BuildConfig.ADMOB_ENABLED) {
            Log.i(TAG, "AdMob disabled by build config")
            return false
        }
        if (BuildConfig.ADMOB_APP_ID.isBlank()) {
            Log.i(TAG, "AdMob app ID not configured")
            return false
        }
        // The interstitial is the app's primary placement (shown on every connect press)
        // and the one that earns, so it alone decides whether AdMob is usable. The
        // banner is optional: [Banner] simply renders nothing until its unit exists.
        // Gating on the banner instead would strand a correctly configured network
        // over a decorative ad slot.
        if (BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID.isBlank()) {
            Log.i(TAG, "AdMob interstitial ad unit ID not configured")
            return false
        }
        if (!hasPlayServices(context)) {
            Log.i(TAG, "Google Play services unavailable, AdMob cannot serve ads")
            return false
        }
        return true
    }

    private fun hasPlayServices(context: Context): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    override fun initialize(context: Context, onReady: () -> Unit, onLoadFailed: () -> Unit) {
        onLoadFailedCallback = onLoadFailed
        if (sdkInitialized) {
            onReady()
            return
        }
        sdkInitialized = true
        // Must be set before initialize(), otherwise the first requests use the
        // default (personalized) configuration.
        MobileAds.setRequestConfiguration(buildRequestConfiguration(personalized.value))
        MobileAds.initialize(context.applicationContext) {
            Log.d(TAG, "Google Mobile Ads SDK initialized")
            onReady()
            preloadInterstitial(context.applicationContext)
        }
    }

    override fun setPersonalized(granted: Boolean) {
        personalized.value = granted
        if (sdkInitialized) {
            MobileAds.setRequestConfiguration(buildRequestConfiguration(granted))
        }
    }

    /**
     * AdMob has no per-user consent API: personalization is expressed through
     * [RequestConfiguration.PublisherPrivacyPersonalizationState], set globally
     * on the SDK. When the user declines, every request is marked as opting out
     * of personalized ads.
     */
    private fun buildRequestConfiguration(granted: Boolean): RequestConfiguration {
        val builder = RequestConfiguration.Builder()
            .setPublisherPrivacyPersonalizationState(
                if (granted) {
                    RequestConfiguration.PublisherPrivacyPersonalizationState.ENABLED
                } else {
                    RequestConfiguration.PublisherPrivacyPersonalizationState.DISABLED
                }
            )
        if (BuildConfig.DEBUG) {
            builder.setTestDeviceIds(testDeviceIds())
        }
        return builder.build()
    }

    /** Emulator always, plus any hashes from `ADMOB_TEST_DEVICE_IDS` in local.properties. */
    private fun testDeviceIds(): List<String> = buildList {
        add(AdRequest.DEVICE_ID_EMULATOR)
        BuildConfig.ADMOB_TEST_DEVICE_IDS.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { add(it) }
    }

    override fun preloadInterstitial(context: Context) {
        val adUnitId = BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID
        if (adUnitId.isBlank()) return
        if (interstitialAd != null || !loading.compareAndSet(false, true)) return
        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loading.set(false)
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading.set(false)
                    Log.w(TAG, "Interstitial ad failed to load: ${error.code} ${error.message}")
                    onLoadFailedCallback()
                }
            }
        )
    }

    override fun showInterstitial(activity: Activity, onClosed: () -> Unit) {
        // Guard so the connect flow continues exactly once even if the SDK
        // reports a dismissal more than once.
        val closed = AtomicBoolean(false)
        val proceed = { if (closed.compareAndSet(false, true)) onClosed() }

        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity.applicationContext)
            proceed()
            return
        }
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                ad.fullScreenContentCallback = null
                preloadInterstitial(activity.applicationContext)
                proceed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial ad failed to show: ${error.code} ${error.message}")
                ad.fullScreenContentCallback = null
                preloadInterstitial(activity.applicationContext)
                proceed()
            }

            override fun onAdClicked() = Unit

            override fun onAdImpression() = Unit
        }
        ad.show(activity)
    }

    @Composable
    override fun Banner(modifier: Modifier) {
        val bannerAdUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
        if (bannerAdUnitId.isBlank()) return
        val isPersonalized = personalized.value
        // key() restarts the subtree when consent changes so the banner is
        // re-requested with the correct personalization setting.
        key(isPersonalized) {
            AndroidView(
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        setAdUnitId(bannerAdUnitId)
                        setAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                // Banner no-fill is common and must not demote the whole
                                // network; only interstitial failures do that.
                                Log.w(TAG, "Banner failed to load: ${error.code} ${error.message}")
                            }
                        })
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = modifier
                    .fillMaxWidth()
                    .height(BANNER_HEIGHT)
            )
        }
    }

    private companion object {
        private const val TAG = "AdmobAdProvider"
        private val BANNER_HEIGHT = 50.dp
    }
}