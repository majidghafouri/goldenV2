package com.goldenv2.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    // DNS Configuration
    val dnsServers: List<String> = listOf("1.1.1.1", "8.8.8.8"),
    val dnsStrategy: DnsStrategy = DnsStrategy.UseSystem,
    val fakeDnsEnabled: Boolean = true,
    val fakeDnsIpRange: String = "198.18.0.0/16",

    // Local Proxy
    val localSocksPort: Int = 10808,
    val localHttpPort: Int = 10809,
    val allowLocalProxy: Boolean = false,

    // Connection
    val autoConnectOnBoot: Boolean = false,
    val autoReconnect: Boolean = true,
    val killSwitchEnabled: Boolean = true,
    val vpnMode: VpnMode = VpnMode.FullTunnel,
    val mtu: Int = 1500,

    // UI
    val theme: ThemeMode = ThemeMode.System,
    val language: String = "en",
    val showSpeedInNotification: Boolean = true,
    val showNotification: Boolean = true,

    // Subscription
    val subscriptionAutoRefresh: Boolean = true,
    val subscriptionRefreshIntervalHours: Int = 24,

    // Logging
    val logLevel: LogLevel = LogLevel.Info,
    val maxLogEntries: Int = 1000,

    // Advanced
    val bypassPackageNames: List<String> = emptyList(),
    val bypassUids: List<Int> = emptyList()
)

@Serializable
enum class DnsStrategy {
    UseSystem, UseCustom, FakeDns
}

@Serializable
enum class VpnMode {
    FullTunnel, // System-wide TUN VPN
    LocalProxyOnly // SOCKS/HTTP on localhost only
}

@Serializable
enum class ThemeMode {
    Light, Dark, System
}

@Serializable
enum class LogLevel {
    Debug, Info, Warning, Error
}