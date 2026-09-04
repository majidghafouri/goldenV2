package com.goldenv2.core.vpn.xray

import com.goldenv2.core.domain.model.AppSettings
import com.goldenv2.core.domain.model.NetworkType
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.RoutingConfig
import com.goldenv2.core.domain.model.Server
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object XrayConfigBuilder {

    private fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map { toJsonElement(it) })
        else -> JsonPrimitive(value.toString())
    }

    private fun jsonMapOf(vararg pairs: Pair<String, Any?>): JsonObject {
        return JsonObject(pairs.map { (k, v) -> k to toJsonElement(v) }.toMap())
    }

    fun buildConfig(
        server: Server,
        settings: AppSettings,
        routing: RoutingConfig,
        localPort: Int = 10808
    ): String {
        val config = jsonMapOf(
            "log" to buildLogConfig(settings.logLevel),
            "dns" to buildDnsConfig(settings),
            "inbounds" to buildInbounds(server, settings, localPort),
            "outbounds" to buildOutbounds(server),
            "routing" to buildRouting(routing)
        )
        return Json.Default.encodeToString(JsonObject.serializer(), config)
    }

    private fun buildLogConfig(logLevel: com.goldenv2.core.domain.model.LogLevel): JsonObject {
        val level = when (logLevel) {
            com.goldenv2.core.domain.model.LogLevel.Debug -> "debug"
            com.goldenv2.core.domain.model.LogLevel.Info -> "info"
            com.goldenv2.core.domain.model.LogLevel.Warning -> "warning"
            com.goldenv2.core.domain.model.LogLevel.Error -> "error"
        }
        return jsonMapOf(
            "loglevel" to level,
            "access" to "",
            "error" to ""
        )
    }

    private fun buildDnsConfig(settings: AppSettings): JsonObject {
        val servers = if (settings.dnsStrategy == com.goldenv2.core.domain.model.DnsStrategy.UseCustom) {
            settings.dnsServers.map { jsonMapOf("address" to it, "port" to 53) }
        } else {
            listOf(
                jsonMapOf("address" to "1.1.1.1", "port" to 53),
                jsonMapOf("address" to "8.8.8.8", "port" to 53)
            )
        }

        val fakedns = if (settings.fakeDnsEnabled) {
            jsonMapOf(
                "enabled" to true,
                "ipPool" to settings.fakeDnsIpRange
            )
        } else {
            jsonMapOf("enabled" to false)
        }

        return jsonMapOf(
            "servers" to servers,
            "fakedns" to fakedns,
            "hosts" to JsonObject(mapOf()),
            "strategy" to when (settings.dnsStrategy) {
                com.goldenv2.core.domain.model.DnsStrategy.UseSystem -> "system"
                com.goldenv2.core.domain.model.DnsStrategy.UseCustom -> "prefer_ipv4"
                com.goldenv2.core.domain.model.DnsStrategy.FakeDns -> "fake_dns"
            }
        )
    }

    private fun buildInbounds(server: Server, settings: AppSettings, localPort: Int): List<JsonObject> {
        val inbounds = mutableListOf<JsonObject>()

        // SOCKS5 inbound
        if (settings.allowLocalProxy) {
            inbounds.add(jsonMapOf(
                "port" to settings.localSocksPort,
                "protocol" to "socks",
                "listen" to "127.0.0.1",
                "settings" to jsonMapOf(
                    "udp" to true,
                    "auth" to "noauth"
                ),
                "sniffing" to jsonMapOf(
                    "enabled" to true,
                    "destOverride" to listOf("http", "tls", "quic")
                )
            ))
        }

        // HTTP inbound
        if (settings.allowLocalProxy) {
            inbounds.add(jsonMapOf(
                "port" to settings.localHttpPort,
                "protocol" to "http",
                "listen" to "127.0.0.1",
                "settings" to jsonMapOf(),
                "sniffing" to jsonMapOf(
                    "enabled" to true,
                    "destOverride" to listOf("http", "tls", "quic")
                )
            ))
        }

        // TUN inbound (for full VPN mode)
        if (settings.vpnMode == com.goldenv2.core.domain.model.VpnMode.FullTunnel) {
            inbounds.add(jsonMapOf(
                "port" to localPort,
                "protocol" to "dokodemo-door",
                "listen" to "127.0.0.1",
                "settings" to jsonMapOf(
                    "network" to "tcp,udp",
                    "followRedirect" to true
                ),
                "sniffing" to jsonMapOf(
                    "enabled" to true,
                    "destOverride" to listOf("http", "tls", "quic")
                )
            ))
        }

        return inbounds
    }

    private fun buildOutbounds(server: Server): List<JsonObject> {
        val mainOutbound = buildMainOutbound(server)
        val directOutbound = jsonMapOf(
            "protocol" to "freedom",
            "tag" to "direct",
            "settings" to jsonMapOf()
        )
        val blockOutbound = jsonMapOf(
            "protocol" to "blackhole",
            "tag" to "block",
            "settings" to jsonMapOf()
        )
        val dnsOutbound = jsonMapOf(
            "protocol" to "dns",
            "tag" to "dns-out",
            "settings" to jsonMapOf()
        )

        return listOf(mainOutbound, directOutbound, blockOutbound, dnsOutbound)
    }

    private fun buildMainOutbound(server: Server): JsonObject {
        return when (server.protocol) {
            Protocol.VMess -> buildVmessOutbound(server)
            Protocol.VLESS -> buildVlessOutbound(server)
            Protocol.Trojan -> buildTrojanOutbound(server)
            Protocol.Shadowsocks -> buildShadowsocksOutbound(server)
            Protocol.Hysteria2 -> buildHysteria2Outbound(server)
            else -> buildVmessOutbound(server) // fallback
        }
    }

    private fun buildVmessOutbound(server: Server): JsonObject {
        val vnext = jsonMapOf(
            "address" to server.address,
            "port" to server.port,
            "users" to listOf(jsonMapOf(
                "id" to (server.uuid ?: ""),
                "alterId" to server.alterId,
                "security" to server.security
            ))
        )

        val streamSettings = buildStreamSettings(server)

        return jsonMapOf(
            "protocol" to "vmess",
            "tag" to "proxy",
            "settings" to jsonMapOf("vnext" to listOf(vnext)),
            "streamSettings" to streamSettings,
            "mux" to jsonMapOf("enabled" to true, "concurrency" to 8)
        )
    }

    private fun buildVlessOutbound(server: Server): JsonObject {
        val vnext = jsonMapOf(
            "address" to server.address,
            "port" to server.port,
            "users" to listOf(jsonMapOf(
                "id" to (server.uuid ?: ""),
                "flow" to (server.flow ?: ""),
                "encryption" to "none"
            ))
        )

        val streamSettings = buildStreamSettings(server)

        return jsonMapOf(
            "protocol" to "vless",
            "tag" to "proxy",
            "settings" to jsonMapOf("vnext" to listOf(vnext)),
            "streamSettings" to streamSettings,
            "mux" to jsonMapOf("enabled" to true, "concurrency" to 8)
        )
    }

    private fun buildTrojanOutbound(server: Server): JsonObject {
        val servers = listOf(jsonMapOf(
            "address" to server.address,
            "port" to server.port,
            "password" to (server.password ?: ""),
            "flow" to ""
        ))

        val streamSettings = buildStreamSettings(server)

        return jsonMapOf(
            "protocol" to "trojan",
            "tag" to "proxy",
            "settings" to jsonMapOf("servers" to servers),
            "streamSettings" to streamSettings,
            "mux" to jsonMapOf("enabled" to true, "concurrency" to 8)
        )
    }

    private fun buildShadowsocksOutbound(server: Server): JsonObject {
        val (method, password) = server.password?.split(":", limit = 2)?.let { (m, p) -> m to p } ?: "aes-256-gcm" to ""

        val servers = listOf(jsonMapOf(
            "address" to server.address,
            "port" to server.port,
            "method" to method,
            "password" to password,
            "plugin" to server.plugin,
            "pluginOpts" to server.pluginOpts
        ))

        return jsonMapOf(
            "protocol" to "shadowsocks",
            "tag" to "proxy",
            "settings" to jsonMapOf("servers" to servers)
        )
    }

    private fun buildHysteria2Outbound(server: Server): JsonObject {
        val streamSettings = jsonMapOf(
            "network" to "udp",
            "security" to "tls",
            "tlsSettings" to jsonMapOf(
                "serverName" to (server.sni ?: server.address),
                "insecure" to false,
                "alpn" to server.alpn
            )
        )

        return jsonMapOf(
            "protocol" to "hysteria2",
            "tag" to "proxy",
            "settings" to jsonMapOf(
                "servers" to listOf(jsonMapOf(
                    "address" to server.address,
                    "port" to server.port,
                    "password" to (server.password ?: ""),
                    "obfs" to server.obfs,
                    "obfsPassword" to server.obfsParam
                ))
            ),
            "streamSettings" to streamSettings
        )
    }

    private fun buildStreamSettings(server: Server): JsonObject {
        val network = server.network.name
        val security = if (server.tls) "tls" else "none"

        val tlsSettings = if (server.tls) {
            val pairs = mutableListOf<Pair<String, Any?>>()
            pairs.add("serverName" to (server.sni ?: server.address))
            pairs.add("allowInsecure" to false)
            if (server.alpn.isNotEmpty()) pairs.add("alpn" to server.alpn)
            pairs.add("fingerprint" to (server.fingerprint ?: "chrome"))
            server.publicKey?.takeIf { it.isNotEmpty() }?.let { pairs.add("publicKey" to it) }
            server.shortId?.takeIf { it.isNotEmpty() }?.let { pairs.add("shortId" to it) }
            server.spiderX?.takeIf { it.isNotEmpty() }?.let { pairs.add("spiderX" to it) }
            jsonMapOf(*pairs.toTypedArray())
        } else null

        val wsSettings = if (network == "ws") {
            jsonMapOf(
                "path" to (server.path ?: "/"),
                "headers" to jsonMapOf("Host" to (server.host ?: server.address))
            )
        } else null

        val grpcSettings = if (network == "grpc") {
            jsonMapOf("serviceName" to (server.path ?: ""))
        } else null

        val httpSettings = if (network == "h2") {
            jsonMapOf(
                "path" to (server.path ?: "/"),
                "host" to listOf(server.host ?: server.address)
            )
        } else null

        return jsonMapOf(
            "network" to network,
            "security" to security,
            "tlsSettings" to tlsSettings,
            "wsSettings" to wsSettings,
            "grpcSettings" to grpcSettings,
            "httpSettings" to httpSettings
        ).let { json -> JsonObject(json.filterValues { it != JsonNull }) }
    }

    private fun buildRouting(routing: RoutingConfig): JsonObject {
        val rules = routing.rules.map { rule ->
            val pairs = mutableListOf<Pair<String, Any?>>()
            pairs.add("type" to "field")
            pairs.add("outboundTag" to rule.outboundTag)
            if (rule.domain.isNotEmpty()) pairs.add("domain" to rule.domain)
            if (rule.ip.isNotEmpty()) pairs.add("ip" to rule.ip)
            if (rule.port != null) pairs.add("port" to rule.port!!)
            if (rule.sourcePort != null) pairs.add("sourcePort" to rule.sourcePort!!)
            if (rule.network != "tcp,udp") pairs.add("network" to rule.network)
            if (rule.source.isNotEmpty()) pairs.add("source" to rule.source)
            if (rule.user.isNotEmpty()) pairs.add("user" to rule.user)
            if (rule.inboundTag.isNotEmpty()) pairs.add("inboundTag" to rule.inboundTag)
            if (rule.protocol.isNotEmpty()) pairs.add("protocol" to rule.protocol)
            rule.attrs?.let { pairs.add("attrs" to it) }
            jsonMapOf(*pairs.toTypedArray())
        }

        return jsonMapOf(
            "domainStrategy" to routing.domainStrategy.name,
            "rules" to rules
        )
    }
}
