package com.goldenv2.feature.servers

/**
 * Decides whether an imported blob is a subscription link or a set of server links.
 *
 * Clipboard paste, QR scanning and manual entry all funnel through the same importer, so
 * this rule has to be right: a subscription URL sent to the server-config parser reports
 * "No valid server links found", which reads as a broken feature to the user.
 */
object ImportSourceDetector {

    private val SERVER_SCHEME = Regex(
        "(vmess|vless|trojan|ss|ssr|hysteria2?|tuic|wireguard|socks)://",
        RegexOption.IGNORE_CASE
    )

    /**
     * True for a bare http(s) URL on a single line that carries no server-link scheme.
     *
     * `http://` is deliberately excluded from the scheme list: it is both a proxy scheme
     * and the scheme of a subscription link, so it cannot disambiguate on its own.
     */
    fun isSubscriptionUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
        if (trimmed.contains('\n') || trimmed.contains('\r')) return false
        if (SERVER_SCHEME.containsMatchIn(trimmed)) return false
        return true
    }
}