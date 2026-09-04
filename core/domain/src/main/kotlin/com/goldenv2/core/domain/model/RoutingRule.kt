package com.goldenv2.core.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.goldenv2.core.data.db.Converters
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(tableName = "routing_rules")
@Serializable
data class RoutingRule(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val type: RuleType,
    val domain: List<String> = emptyList(),
    val ip: List<String> = emptyList(),
    val port: String? = null,
    val sourcePort: String? = null,
    val network: String = "tcp,udp",
    val source: List<String> = emptyList(),
    val user: List<String> = emptyList(),
    val inboundTag: List<String> = emptyList(),
    val protocol: List<String> = emptyList(),
    val attrs: String? = null,
    val outboundTag: String,
    val enabled: Boolean = true,
    val order: Int = 0,
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now()
)

@Serializable
enum class RuleType {
    field, // Standard Xray routing rule
    custom // Custom user-defined rule
}

@Serializable
data class RoutingConfig(
    val domainStrategy: DomainStrategy = DomainStrategy.AsIs,
    val rules: List<RoutingRule> = emptyList(),
    val balancers: List<BalancerConfig> = emptyList()
) {
    companion object {
        fun default(): RoutingConfig = RoutingConfig(
            domainStrategy = DomainStrategy.AsIs,
            rules = listOf(
                // Bypass LAN
                RoutingRule(
                    type = RuleType.field,
                    ip = listOf("geoip:private"),
                    outboundTag = "direct",
                    order = 0
                ),
                // Proxy all other traffic
                RoutingRule(
                    type = RuleType.field,
                    outboundTag = "proxy",
                    order = 100
                )
            )
        )

        fun proxyAll(): RoutingConfig = RoutingConfig(
            domainStrategy = DomainStrategy.AsIs,
            rules = listOf(
                RoutingRule(
                    type = RuleType.field,
                    outboundTag = "proxy",
                    order = 0
                )
            )
        )

        fun directAll(): RoutingConfig = RoutingConfig(
            domainStrategy = DomainStrategy.AsIs,
            rules = listOf(
                RoutingRule(
                    type = RuleType.field,
                    outboundTag = "direct",
                    order = 0
                )
            )
        )
    }
}

@Serializable
enum class DomainStrategy {
    AsIs, IPIfNonMatch, IPOnDemand
}

@Serializable
data class BalancerConfig(
    val tag: String,
    val selector: List<String> = emptyList()
)