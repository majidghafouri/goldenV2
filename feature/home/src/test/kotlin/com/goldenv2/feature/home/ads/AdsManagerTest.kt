package com.goldenv2.feature.home.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

/**
 * Covers the AdMob -> Yandex fallback contract in [AdsManager].
 */
class AdsManagerTest {

    private val context = Mockito.mock(Context::class.java).apply {
        Mockito.`when`(applicationContext).thenReturn(this)
    }
    private val activity = Mockito.mock(Activity::class.java)

    private fun manager(
        personalizationAllowed: Boolean = true,
        vararg providers: AdProvider
    ) = AdsManager({ personalizationAllowed }, providers.toList())

    @Test
    fun `uses AdMob when it is available`() {
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = true)
        val yandex = FakeAdProvider(AdNetwork.YANDEX, available = true)

        val manager = manager(providers = arrayOf(admob, yandex))
        manager.initialize(context)

        assertSame(admob, manager.activeProvider.value)
        assertFalse(yandex.initialized)
    }

    @Test
    fun `falls back to Yandex when AdMob is not available`() {
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = false)
        val yandex = FakeAdProvider(AdNetwork.YANDEX, available = true)

        val manager = manager(providers = arrayOf(admob, yandex))
        manager.initialize(context)

        assertSame(yandex, manager.activeProvider.value)
        assertFalse(admob.initialized)
        assertTrue(yandex.initialized)
    }

    @Test
    fun `keeps AdMob while load failures are within the threshold`() {
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = true)
        val yandex = FakeAdProvider(AdNetwork.YANDEX, available = true)

        val manager = manager(providers = arrayOf(admob, yandex))
        manager.initialize(context)

        admob.reportLoadFailure()
        admob.reportLoadFailure()

        assertSame(admob, manager.activeProvider.value)
        assertFalse(yandex.initialized)
    }

    @Test
    fun `falls back to Yandex when AdMob stops filling`() {
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = true)
        val yandex = FakeAdProvider(AdNetwork.YANDEX, available = true)

        val manager = manager(providers = arrayOf(admob, yandex))
        manager.initialize(context)
        repeat(MAX_LOAD_FAILURES) { admob.reportLoadFailure() }

        assertSame(yandex, manager.activeProvider.value)
        assertTrue(yandex.initialized)
    }

    @Test
    fun `shows no ads when every network is unavailable`() {
        val manager = manager(
            providers = arrayOf(
                FakeAdProvider(AdNetwork.ADMOB, available = false),
                FakeAdProvider(AdNetwork.YANDEX, available = false)
            )
        )
        manager.initialize(context)

        assertSame(NoAdsProvider, manager.activeProvider.value)
    }

    @Test
    fun `uses AdMob with only the interstitial configured`() {
        // The banner unit is optional: without it the network still earns on interstitials.
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = true)
        val manager = manager(providers = arrayOf(admob))
        manager.initialize(context)

        assertSame(admob, manager.activeProvider.value)
    }

    @Test
    fun `never blocks the connect flow when no ad is ready`() {
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = true).apply { hasAd = false }
        val manager = manager(providers = arrayOf(admob))
        manager.initialize(context)

        var proceeded = false
        manager.showAdOrProceed(activity) { proceeded = true }

        assertTrue(proceeded)
        assertEquals(0, admob.showCount)
    }

    @Test
    fun `connects after the interstitial is dismissed`() {
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = true).apply { hasAd = true }
        val manager = manager(providers = arrayOf(admob))
        manager.initialize(context)

        var proceeded = false
        manager.showAdOrProceed(activity) { proceeded = true }

        assertEquals(1, admob.showCount)
        assertTrue(proceeded)
    }

    @Test
    fun `connects immediately when no network is serving`() {
        val manager = manager(providers = arrayOf(FakeAdProvider(AdNetwork.ADMOB, available = false)))
        manager.initialize(context)

        var proceeded = false
        manager.showAdOrProceed(activity) { proceeded = true }

        assertTrue(proceeded)
    }

    @Test
    fun `forwards a consent change to every provider`() {
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = true)
        val yandex = FakeAdProvider(AdNetwork.YANDEX, available = true)
        val manager = manager(providers = arrayOf(admob, yandex))

        manager.onConsentChanged(false)

        assertEquals(false, admob.personalized)
        assertEquals(false, yandex.personalized)
    }

    @Test
    fun `applies stored consent before initializing the chosen network`() {
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = true)

        manager(personalizationAllowed = false, providers = arrayOf(admob)).initialize(context)

        assertEquals(false, admob.personalized)
    }

    @Test
    fun `initializes only once`() {
        val admob = FakeAdProvider(AdNetwork.ADMOB, available = true)
        val manager = manager(providers = arrayOf(admob))

        manager.initialize(context)
        manager.initialize(context)

        assertEquals(1, admob.initializeCount)
    }

    private companion object {
        /** Mirrors AdsManager.MAX_CONSECUTIVE_LOAD_FAILURES. */
        const val MAX_LOAD_FAILURES = 3
    }
}

/** Minimal [AdProvider] that records what [AdsManager] asked it to do. */
private class FakeAdProvider(
    override val network: AdNetwork,
    private val available: Boolean
) : AdProvider {

    var initialized = false
        private set
    var initializeCount = 0
        private set
    var personalized: Boolean? = null
        private set
    var hasAd = true
    var showCount = 0
        private set

    private lateinit var onLoadFailed: () -> Unit

    override fun isAvailable(context: Context): Boolean = available

    override fun initialize(context: Context, onReady: () -> Unit, onLoadFailed: () -> Unit) {
        initializeCount++
        this.onLoadFailed = onLoadFailed
        initialized = true
        onReady()
    }

    fun reportLoadFailure() {
        if (::onLoadFailed.isInitialized) onLoadFailed()
    }

    override fun setPersonalized(granted: Boolean) {
        personalized = granted
    }

    override fun preloadInterstitial(context: Context) = Unit

    /** Closes immediately, standing in for the user dismissing the ad. */
    override fun showInterstitial(activity: Activity, onClosed: () -> Unit) {
        if (!hasAd) return onClosed()
        showCount++
        onClosed()
    }

    @Composable
    override fun Banner(modifier: Modifier) = Unit
}