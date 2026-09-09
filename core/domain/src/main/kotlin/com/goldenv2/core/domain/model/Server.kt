package com.goldenv2.core.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.goldenv2.core.data.db.Converters
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(tableName = "servers")
@Serializable
data class Server(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val subscriptionId: String,
    val name: String,
    val protocol: Protocol,
    val address: String,
    val port: Int,
    val uuid: String? = null, // VMess/VLESS UUID
    val password: String? = null, // SS/Trojan password
    val alterId: Int = 0, // VMess
    val security: String = "auto", // VMess security
    val network: NetworkType = NetworkType.tcp,
    val tls: Boolean = false,
    val sni: String? = null,
    val alpn: List<String> = emptyList(),
    val fingerprint: String? = null,
    val path: String? = null, // WS/HTTP path
    val host: String? = null, // WS/HTTP host
    val headerType: String? = null, // HTTP header type
    val flow: String? = null, // VLESS flow
    val publicKey: String? = null, // Reality/Hysteria2
    val shortId: String? = null, // Reality
    val spiderX: String? = null, // Reality
    val obfs: String? = null, // SS obfs
    val obfsParam: String? = null, // SS obfs param
    val plugin: String? = null, // SS plugin
    val pluginOpts: String? = null, // SS plugin opts
    val remark: String? = null, // User remark
    val group: String? = null, // Server group
    val latency: Long = -1, // Ping latency in ms, -1 = unknown
    val isSelected: Boolean = false,
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now()
) {
    fun copyWithLatency(newLatency: Long): Server = copy(latency = newLatency, updatedAt = Instant.now())
    fun copySelected(selected: Boolean): Server = copy(isSelected = selected, updatedAt = Instant.now())
}

@Serializable
enum class Protocol {
    VMess, VLESS, Trojan, Shadowsocks, Hysteria2, Hysteria, Tuic, WireGuard, Ssh
}

@Serializable
enum class NetworkType {
    tcp, kcp, ws, h2, quic, grpc, httpupgrade
}

@Serializable
data class ServerConfig(
    val protocol: Protocol,
    val address: String,
    val port: Int,
    val settings: Map<String, String>,
    val streamSettings: Map<String, String>? = null
)