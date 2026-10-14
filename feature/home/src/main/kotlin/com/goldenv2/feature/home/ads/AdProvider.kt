package com.goldenv2.feature.home.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Ad networks the app can monetize with, listed in fallback order. */
enum class AdNetwork {
    ADMOB,
    YANDEX,

    /** Nothing to show; used before a network is chosen or when every network failed. */
    NONE
}

/**
 * Abstraction over a single ad network SDK.
 *
 * Implementations must never block the VPN connect flow: when no ad is ready,
 * or loading/showing fails, [showInterstitial] still invokes [onClosed].
 */
interface AdProvider {

    val network: AdNetwork

    /**
     * Whether this network can actually serve ads on the current device and build.
     * Checked before the SDK is initialized.
     */
    fun isAvailable(context: Context): Boolean

    /**
     * Initializes the SDK and, once it is ready, preloads the interstitial.
     *
     * @param onReady invoked after initialization finished, successful or not.
     * @param onLoadFailed invoked on every failed interstitial load attempt. The
     *   orchestrator uses it to demote the network to the next fallback.
     */
    fun initialize(context: Context, onReady: () -> Unit, onLoadFailed: () -> Unit)

    /**
     * Applies the user's data-processing consent.
     * `false` means ads must be requested non-personalized.
     */
    fun setPersonalized(granted: Boolean)

    fun preloadInterstitial(context: Context)

    /**
     * Shows the preloaded interstitial and invokes [onClosed] exactly once, when the
     * ad is dismissed or when no ad could be shown. Must be called on the main thread.
     */
    fun showInterstitial(activity: Activity, onClosed: () -> Unit)

    /** Renders the banner, or nothing at all when the network cannot serve one. */
    @Composable
    fun Banner(modifier: Modifier)
}

/**
 * Fallback used before a network has been selected and after every network was ruled out.
 * Renders nothing and never blocks the connect flow.
 */
object NoAdsProvider : AdProvider {

    override val network: AdNetwork = AdNetwork.NONE

    override fun isAvailable(context: Context): Boolean = false

    override fun initialize(context: Context, onReady: () -> Unit, onLoadFailed: () -> Unit) = onReady()

    override fun setPersonalized(granted: Boolean) = Unit

    override fun preloadInterstitial(context: Context) = Unit

    override fun showInterstitial(activity: Activity, onClosed: () -> Unit) = onClosed()

    @Composable
    override fun Banner(modifier: Modifier) = Unit
}