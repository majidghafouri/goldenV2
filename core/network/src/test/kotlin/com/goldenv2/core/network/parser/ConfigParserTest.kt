package com.goldenv2.core.network.parser

import com.goldenv2.core.domain.model.NetworkType
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigParserTest {

    @Test
    fun `parse valid VMess URI`() {
        val vmessJson = """
            {
                "v": "2",
                "ps": "Test VMess",
                "add": "example.com",
                "port": "443",
                "id": "12345678-1234-1234-1234-123456789012",
                "aid": "0",
                "scy": "auto",
                "net": "ws",
                "type": "none",
                "host": "example.com",
                "path": "/vmess",
                "tls": "tls",
                "sni": "example.com",
                "alpn": "h2,http/1.1",
                "fp": "chrome"
            }
        """.trimIndent()

        val base64 = android.util.Base64.encodeToString(vmessJson.toByteArray(), android.util.Base64.NO_WRAP)
        val uri = "vmess://$base64"

        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).server
        assertEquals(Protocol.VMess, server.protocol)
        assertEquals("example.com", server.address)
        assertEquals(443, server.port)
        assertEquals("12345678-1234-1234-1234-123456789012", server.uuid)
        assertEquals(NetworkType.ws, server.network)
        assertEquals("example.com", server.host)
        assertEquals("/vmess", server.path)
        assertTrue(server.tls)
        assertEquals("example.com", server.sni)
        assertEquals(listOf("h2", "http/1.1"), server.alpn)
        assertEquals("chrome", server.fingerprint)
    }

    @Test
    fun `parse valid VLESS URI`() {
        val uri = "vless://12345678-1234-1234-1234-123456789012@example.com:443?type=ws&security=tls&sni=example.com&fp=chrome&flow=xtls-rprx-vision#Test%20VLESS"

        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).server
        assertEquals(Protocol.VLESS, server.protocol)
        assertEquals("example.com", server.address)
        assertEquals(443, server.port)
        assertEquals("12345678-1234-1234-1234-123456789012", server.uuid)
        assertEquals(NetworkType.ws, server.network)
        assertTrue(server.tls)
        assertEquals("example.com", server.sni)
        assertEquals("chrome", server.fingerprint)
        assertEquals("xtls-rprx-vision", server.flow)
        assertEquals("Test VLESS", server.name)
    }

    @Test
    fun `parse valid Trojan URI`() {
        val uri = "trojan://password123@example.com:443?type=tcp&security=tls&sni=example.com&allowInsecure=1#Test%20Trojan"

        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).server
        assertEquals(Protocol.Trojan, server.protocol)
        assertEquals("example.com", server.address)
        assertEquals(443, server.port)
        assertEquals("password123", server.password)
        assertEquals(NetworkType.tcp, server.network)
        assertTrue(server.tls)
        assertEquals("example.com", server.sni)
        assertEquals("Test Trojan", server.name)
    }

    @Test
    fun `parse valid Shadowsocks URI with base64`() {
        val userInfo = "aes-256-gcm:password123"
        val base64 = android.util.Base64.encodeToString(userInfo.toByteArray(), android.util.Base64.NO_WRAP)
        val uri = "ss://$base64@example.com:8388#Test%20SS"

        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).server
        assertEquals(Protocol.Shadowsocks, server.protocol)
        assertEquals("example.com", server.address)
        assertEquals(8388, server.port)
        assertEquals("aes-256-gcm:password123", server.password)
        assertEquals("Test SS", server.name)
    }

    @Test
    fun `parse valid Shadowsocks URI without base64`() {
        val uri = "ss://aes-256-gcm:password123@example.com:8388#Test%20SS"

        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).server
        assertEquals(Protocol.Shadowsocks, server.protocol)
        assertEquals("example.com", server.address)
        assertEquals(8388, server.port)
        assertEquals("aes-256-gcm:password123", server.password)
    }

    @Test
    fun `parse valid Hysteria2 URI`() {
        val uri = "hysteria2://password123@example.com:443?sni=example.com&insecure=0&alpn=h3&obfs=salamander&obfs-password=obfsPass#Test%20Hysteria2"

        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).server
        assertEquals(Protocol.Hysteria2, server.protocol)
        assertEquals("example.com", server.address)
        assertEquals(443, server.port)
        assertEquals("password123", server.password)
        assertEquals("example.com", server.sni)
        assertTrue(server.tls)
        assertEquals(listOf("h3"), server.alpn)
        assertEquals("salamander", server.obfs)
        assertEquals("obfsPass", server.obfsParam)
        assertEquals("Test Hysteria2", server.name)
    }

    @Test
    fun `parse subscription content`() {
        val server1Json = """{"v":"2","ps":"Server1","add":"server1.com","port":"443","id":"11111111-1111-1111-1111-111111111111","aid":"0","scy":"auto","net":"tcp","type":"none","tls":"tls"}"""
        val server2Json = """{"v":"2","ps":"Server2","add":"server2.com","port":"8443","id":"22222222-2222-2222-2222-222222222222","aid":"0","scy":"auto","net":"tcp","type":"none","tls":"tls"}"""

        val base64_1 = android.util.Base64.encodeToString(server1Json.toByteArray(), android.util.Base64.NO_WRAP)
        val base64_2 = android.util.Base64.encodeToString(server2Json.toByteArray(), android.util.Base64.NO_WRAP)

        val subscriptionContent = android.util.Base64.encodeToString("$base64_1\n$base64_2".toByteArray(), android.util.Base64.NO_WRAP)

        val servers = parseSubscriptionContent(subscriptionContent)

        assertEquals(2, servers.size)
        assertEquals("Server1", servers[0].name)
        assertEquals("server1.com", servers[0].address)
        assertEquals("Server2", servers[1].name)
        assertEquals("server2.com", servers[1].address)
    }

    @Test
    fun `parse invalid URI returns failure`() {
        val result = ConfigParserRegistry.parse("invalid://uri")
        assertTrue(result is ParseResult.Failure)
    }

    @Test
    fun `unsupported protocol returns failure`() {
        val result = ConfigParserRegistry.parse("unknown://example.com")
        assertTrue(result is ParseResult.Failure)
    }
}