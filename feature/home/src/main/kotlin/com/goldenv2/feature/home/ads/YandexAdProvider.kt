package com.goldenv2.feature.home.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goldenv2.feature.home.BuildConfig
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.compose.Banner
import com.yandex.mobile.ads.compose.BannerEvents
import com.yandex.mobile.ads.compose.BannerSize
import com.yandex.mobile.ads.compose.rememberBannerAdState
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Yandex Mobile Ads implementation of [AdProvider], used whenever AdMob is not
 * available (not configured, no Google Play Services on the device, or no fill).
 *
 * Like every [AdProvider], it never blocks the VPN connect flow: if no ad is
 * ready, or loading/showing fails, [showInterstitial] still invokes [onClosed].
 */
@Singleton
class YandexAdProvider @Inject constructor() : AdProvider {

    override val network: AdNetwork = AdNetwork.YANDEX

    private var interstitialAdLoader: InterstitialAdLoader? = null
    private var interstitialAd: InterstitialAd? = null
    private val loading = AtomicBoolean(false)
    private var onLoadFailedCallback: () -> Unit = {}

    /** Read during composition so a consent change re-creates the banner. */
    private val personalized = mutableStateOf(false)

    @Volatile
    private var sdkInitialized = false

    /** Yandex Mobile Ads works on every device, so it is always a valid fallback. */
    override fun isAvailable(context: Context): Boolean = true

    override fun initialize(context: Context, onReady: () -> Unit, onLoadFailed: () -> Unit) {
        onLoadFailedCallback = onLoadFailed
        // Apply persisted consent before loading any ads.
        YandexAds.setUserConsent(personalized.value)
        if (sdkInitialized) {
            onReady()
            return
        }
        sdkInitialized = true
        YandexAds.initialize(context) {
            Log.d(TAG, "Yandex Mobile Ads SDK initialized")
            onReady()
            preloadInterstitial(context.applicationContext)
        }
    }

    override fun setPersonalized(granted: Boolean) {
        personalized.value = granted
        YandexAds.setUserConsent(granted)
    }

    override fun preloadInterstitial(context: Context) {
        if (INTERSTITIAL_AD_UNIT_ID.isBlank()) return
        if (interstitialAd != null || !loading.compareAndSet(false, true)) return
        val loader = interstitialAdLoader ?: InterstitialAdLoader(context.applicationContext).also {
            interstitialAdLoader = it
        }
        val request = AdRequest.Builder(INTERSTITIAL_AD_UNIT_ID).build()
        loader.loadAd(request, object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {
                loading.set(false)
                interstitialAd = ad
                Log.d(TAG, "Interstitial ad loaded")
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                loading.set(false)
                Log.w(TAG, "Interstitial ad failed to load: ${error.code} ${error.description}")
                onLoadFailedCallback()
            }
        })
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
        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() {
                Log.d(TAG, "Interstitial ad shown")
            }

            override fun onAdFailedToShow(adError: AdError) {
                Log.w(TAG, "Interstitial ad failed to show: ${adError.description}")
                ad.setAdEventListener(null)
                preloadInterstitial(activity.applicationContext)
                proceed()
            }

            override fun onAdDismissed() {
                ad.setAdEventListener(null)
                preloadInterstitial(activity.applicationContext)
                proceed()
            }

            override fun onAdClicked() = Unit

            override fun onAdImpression(impressionData: ImpressionData?) = Unit
        })
        ad.show(activity)
    }

    @Composable
    override fun Banner(modifier: Modifier) {
        if (BANNER_AD_UNIT_ID.isBlank()) return
        val isPersonalized = personalized.value
        // key() restarts the subtree when consent changes so the banner is
        // re-requested with the correct personalization setting.
        key(isPersonalized) {
            val bannerState = rememberBannerAdState(
                adSize = BannerSize.Sticky(width = 320.dp),
                events = BannerEvents()
            )
            LaunchedEffect(bannerState) {
                bannerState.loadAd(AdRequest.Builder(BANNER_AD_UNIT_ID).build())
            }
            Banner(
                state = bannerState,
                modifier = modifier.fillMaxWidth()
            )
        }
    }

    private companion object {
        private const val TAG = "YandexAdProvider"
        private val INTERSTITIAL_AD_UNIT_ID = BuildConfig.YANDEX_INTERSTITIAL_AD_UNIT_ID
        private val BANNER_AD_UNIT_ID = BuildConfig.YANDEX_BANNER_AD_UNIT_ID
    }
}