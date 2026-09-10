package com.goldenv2.feature.home.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.goldenv2.feature.home.BuildConfig
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preloads a Yandex interstitial ad and shows it when the user presses Connect.
 *
 * The VPN connection itself must never be blocked on the ad: if the ad is not
 * ready, fails to load or fails to show, [showAdOrProceed] invokes [onProceed]
 * immediately so the connect flow continues uninterrupted.
 */
@Singleton
class AdsManager @Inject constructor(
    private val consentManager: ConsentManager
) {

    private var interstitialAdLoader: InterstitialAdLoader? = null
    private var interstitialAd: InterstitialAd? = null
    private val loading = AtomicBoolean(false)

    @Volatile
    private var sdkInitialized = false

    fun initialize(context: Context) {
        if (sdkInitialized) return
        sdkInitialized = true
        // Apply persisted consent before loading any ads.
        YandexAds.setUserConsent(consentManager.isPersonalizationAllowed)
        YandexAds.initialize(context) {
            Log.d(TAG, "Yandex Mobile Ads SDK initialized")
            preloadInterstitial(context.applicationContext)
        }
    }

    fun preloadInterstitial(context: Context) {
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
            }
        })
    }

    /**
     * Shows the preloaded interstitial, then calls [onProceed] once the ad is
     * dismissed (or immediately if no ad is available / showing fails).
     * Must be called from the main thread with a foreground [activity].
     */
    fun showAdOrProceed(activity: Activity, onProceed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity.applicationContext)
            onProceed()
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
                onProceed()
            }

            override fun onAdDismissed() {
                ad.setAdEventListener(null)
                preloadInterstitial(activity.applicationContext)
                onProceed()
            }

            override fun onAdClicked() = Unit

            override fun onAdImpression(impressionData: ImpressionData?) = Unit
        })
        ad.show(activity)
    }

    private companion object {
        private const val TAG = "AdsManager"
        private val INTERSTITIAL_AD_UNIT_ID = BuildConfig.YANDEX_INTERSTITIAL_AD_UNIT_ID
    }
}
