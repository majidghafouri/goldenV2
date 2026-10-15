package com.goldenv2.feature.servers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The importer routes on this decision, and getting it wrong makes paste and QR of a
 * subscription link report "No valid server links found".
 */
class ImportSourceDetectorTest {

    @Test
    fun `plain https url is a subscription`() {
        assertTrue(ImportSourceDetector.isSubscriptionUrl("https://example.com/sub/abc123"))
    }

    @Test
    fun `plain http url is a subscription`() {
        assertTrue(ImportSourceDetector.isSubscriptionUrl("http://example.com/sub"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertTrue(ImportSourceDetector.isSubscriptionUrl("  https://example.com/sub  "))
    }

    @Test
    fun `server link is not a subscription`() {
        assertFalse(ImportSourceDetector.isSubscriptionUrl("vless://uuid@example.com:443?security=tls#Node"))
    }

    @Test
    fun `vmess link is not a subscription`() {
        assertFalse(ImportSourceDetector.isSubscriptionUrl("vmess://eyJhZGQiOiIxLjIuMy40In0="))
    }

    @Test
    fun `shadowsocks link is not a subscription`() {
        assertFalse(ImportSourceDetector.isSubscriptionUrl("ss://YWVzLTEyOC1nY206cGFzcw@1.2.3.4:8388"))
    }

    @Test
    fun `scheme is matched case-insensitively`() {
        assertFalse(ImportSourceDetector.isSubscriptionUrl("VLESS://uuid@example.com:443"))
    }

    @Test
    fun `a subscription url embedded in a server list is not treated as a subscription`() {
        val blob = "vless://uuid@example.com:443\nhttps://example.com/sub"
        assertFalse(ImportSourceDetector.isSubscriptionUrl(blob))
    }

    @Test
    fun `multi-line base64 blob is not a subscription`() {
        assertFalse(ImportSourceDetector.isSubscriptionUrl("dmxlc3M6Ly8...\nhttps://other.example"))
    }

    @Test
    fun `non url text is not a subscription`() {
        assertFalse(ImportSourceDetector.isSubscriptionUrl("just some words"))
        assertFalse(ImportSourceDetector.isSubscriptionUrl(""))
    }
}