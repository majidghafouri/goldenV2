package com.goldenv2.core.vpn.xray

import com.goldenv2.core.domain.model.AppSettings
import com.goldenv2.core.domain.model.LogLevel
import com.goldenv2.core.domain.model.NetworkType
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.RoutingConfig
import com.goldenv2.core.domain.model.RoutingRule
import com.goldenv2.core.domain.model.RuleType
import com.goldenv2.core.domain.model.Server
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XrayConfigBuilderTest {

    private val server = Server(
        subscriptionId = "sub-1",
        name = "test",
        protocol = Protocol.VLESS,
        address = "127.0.0.1",
        port = 443,
        uuid = "00000000-0000-0000-0000-000000000000",
        network = NetworkType.tcp
    )

    private fun build(routing: RoutingConfig): JsonObject {
        val configJson = XrayConfigBuilder.buildConfig(
            server = server,
            settings = AppSettings(logLevel = LogLevel.Info),
            routing = routing,
            geoipPath = "/data/files/geoip.dat",
            geositePath = "/data/files/geosite.dat"
        )
        return Json.parseToJsonElement(configJson).jsonObject
    }

    @Test
    fun `default routing - catch-all rule emits network field`() {
        val routingObj = build(RoutingConfig.default()).getValue("routing").jsonObject
        val rules = routingObj.getValue("rules").jsonArray

        assertEquals(2, rules.size)

        val bypass = rules[0].jsonObject
        assertEquals("direct", bypass.getValue("outboundTag").jsonPrimitive.content)
        assertTrue(
            "rule with geoip:private must keep its ip field",
            bypass.getValue("ip").jsonArray.any { it.jsonPrimitive.content == "geoip:private" }
        )
        assertEquals("tcp,udp", bypass.getValue("network").jsonPrimitive.content)

        val catchAll = rules[1].jsonObject
        assertEquals("proxy", catchAll.getValue("outboundTag").jsonPrimitive.content)
        assertTrue(
            "catch-all rule with no other matchers must emit network so Xray does not reject it",
            catchAll.containsKey("network")
        )
        assertEquals("tcp,udp", catchAll.getValue("network").jsonPrimitive.content)
    }

    @Test
    fun `proxyAll routing - sole rule emits network field`() {
        val rules = build(RoutingConfig.proxyAll()).getValue("routing").jsonObject
            .getValue("rules").jsonArray

        assertEquals(1, rules.size)
        val rule = rules[0].jsonObject
        assertEquals("proxy", rule.getValue("outboundTag").jsonPrimitive.content)
        assertTrue("sole catch-all rule must emit network", rule.containsKey("network"))
    }

    @Test
    fun `routing includes geoip and geosite paths`() {
        val routingObj = build(RoutingConfig.default()).getValue("routing").jsonObject

        assertEquals(
            "/data/files/geoip.dat",
            routingObj.getValue("geoip").jsonObject.getValue("path").jsonPrimitive.content
        )
        assertEquals(
            "/data/files/geosite.dat",
            routingObj.getValue("geosite").jsonObject.getValue("path").jsonPrimitive.content
        )
    }

    @Test
    fun `rule with custom network keeps its value`() {
        val routing = RoutingConfig(
            rules = listOf(
                RoutingRule(
                    type = RuleType.field,
                    network = "udp",
                    domain = listOf("example.com"),
                    outboundTag = "direct"
                )
            )
        )
        val rules = build(routing).getValue("routing").jsonObject.getValue("rules").jsonArray

        assertEquals(1, rules.size)
        val rule = rules[0].jsonObject
        assertEquals("udp", rule.getValue("network").jsonPrimitive.content)
        assertTrue(rule.getValue("domain").jsonArray.isNotEmpty())
    }

    @Test
    fun `custom field rule with no matchers emits network`() {
        val routing = RoutingConfig(
            rules = listOf(
                RoutingRule(type = RuleType.field, outboundTag = "block")
            )
        )
        val rules = build(routing).getValue("routing").jsonObject.getValue("rules").jsonArray
        assertEquals(1, rules.size)
        val rule = rules[0].jsonObject
        assertEquals("block", rule.getValue("outboundTag").jsonPrimitive.content)
        assertTrue(
            "any field rule must emit network regardless of its other matchers",
            rule.containsKey("network")
        )
        assertEquals("tcp,udp", rule.getValue("network").jsonPrimitive.content)
    }
}