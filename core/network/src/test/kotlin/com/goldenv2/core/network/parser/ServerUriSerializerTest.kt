package com.goldenv2.core.network.parser

import com.goldenv2.core.domain.model.NetworkType
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerUriSerializerTest {

    @Test
    fun `round trip VLESS server`() {
        val server = Server(
            subscriptionId = "",
            name = "My VLESS",
            protocol = Protocol.VLESS,
            address = "example.com",
            port = 443,
            uuid = "12345678-1234-1234-1234-123456789012",
            network = NetworkType.ws,
            tls = true,
            sni = "example.com",
            fingerprint = "chrome",
            flow = "xtls-rprx-vision",
            path = "/ws",
            host = "example.com"
        )

        val uri = ServerUriSerializer.toUri(server)
        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success, "Re-parse failed: ${(result as? ParseResult.Failure)?.error}")
        val parsed = (result as ParseResult.Success).server
        assertEquals("My VLESS", parsed.name)
        assertEquals("example.com", parsed.address)
        assertEquals(443, parsed.port)
        assertEquals(server.uuid, parsed.uuid)
        assertEquals(NetworkType.ws, parsed.network)
        assertTrue(parsed.tls)
        assertEquals("example.com", parsed.sni)
        assertEquals("chrome", parsed.fingerprint)
        assertEquals("xtls-rprx-vision", parsed.flow)
        assertEquals("/ws", parsed.path)
    }

    @Test
    fun `round trip VMess server`() {
        val server = Server(
            subscriptionId = "",
            name = "My VMess",
            protocol = Protocol.VMess,
            address = "vmess.example.com",
            port = 8443,
            uuid = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            alterId = 4,
            security = "auto",
            network = NetworkType.ws,
            tls = true,
            sni = "vmess.example.com",
            host = "vmess.example.com",
            path = "/vmess",
            alpn = listOf("h2", "http/1.1"),
            fingerprint = "chrome"
        )

        val uri = ServerUriSerializer.toUri(server)
        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).server
        assertEquals("My VMess", parsed.name)
        assertEquals("vmess.example.com", parsed.address)
        assertEquals(8443, parsed.port)
        assertEquals(server.uuid, parsed.uuid)
        assertEquals(4, parsed.alterId)
        assertEquals(NetworkType.ws, parsed.network)
        assertTrue(parsed.tls)
        assertEquals("vmess.example.com", parsed.host)
        assertEquals("/vmess", parsed.path)
        assertEquals(listOf("h2", "http/1.1"), parsed.alpn)
        assertEquals("chrome", parsed.fingerprint)
    }

    @Test
    fun `round trip Shadowsocks server`() {
        val server = Server(
            subscriptionId = "",
            name = "My SS",
            protocol = Protocol.Shadowsocks,
            address = "ss.example.com",
            port = 8388,
            password = "aes-256-gcm:secretpass"
        )

        val uri = ServerUriSerializer.toUri(server)
        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).server
        assertEquals("My SS", parsed.name)
        assertEquals("ss.example.com", parsed.address)
        assertEquals(8388, parsed.port)
        assertEquals("aes-256-gcm:secretpass", parsed.password)
    }

    @Test
    fun `round trip Trojan server`() {
        val server = Server(
            subscriptionId = "",
            name = "My Trojan",
            protocol = Protocol.Trojan,
            address = "trojan.example.com",
            port = 443,
            password = "trojanpass",
            network = NetworkType.tcp,
            tls = true,
            sni = "trojan.example.com"
        )

        val uri = ServerUriSerializer.toUri(server)
        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).server
        assertEquals("My Trojan", parsed.name)
        assertEquals("trojan.example.com", parsed.address)
        assertEquals(443, parsed.port)
        assertEquals("trojanpass", parsed.password)
        assertEquals(NetworkType.tcp, parsed.network)
        assertTrue(parsed.tls)
    }

    @Test
    fun `round trip Hysteria2 server`() {
        val server = Server(
            subscriptionId = "",
            name = "My Hy2",
            protocol = Protocol.Hysteria2,
            address = "hy2.example.com",
            port = 8443,
            password = "hy2pass",
            sni = "hy2.example.com",
            alpn = listOf("h3"),
            obfs = "salamander",
            obfsParam = "obfspass"
        )

        val uri = ServerUriSerializer.toUri(server)
        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).server
        assertEquals("My Hy2", parsed.name)
        assertEquals("hy2.example.com", parsed.address)
        assertEquals(8443, parsed.port)
        assertEquals("hy2pass", parsed.password)
        assertEquals("hy2.example.com", parsed.sni)
        assertEquals("salamander", parsed.obfs)
        assertEquals("obfspass", parsed.obfsParam)
    }

    @Test
    fun `round trip SSH server`() {
        val server = Server(
            subscriptionId = "",
            name = "My SSH",
            protocol = Protocol.Ssh,
            address = "ssh.example.com",
            port = 22,
            uuid = "root",
            password = "sshpass",
            path = "/sdcard/key.pem",
            host = "keypassphrase"
        )

        val uri = ServerUriSerializer.toUri(server)
        val result = ConfigParserRegistry.parse(uri)

        assertTrue(result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).server
        assertEquals("My SSH", parsed.name)
        assertEquals("ssh.example.com", parsed.address)
        assertEquals(22, parsed.port)
        assertEquals("root", parsed.uuid)
        assertEquals("sshpass", parsed.password)
        assertEquals("/sdcard/key.pem", parsed.path)
        assertEquals("keypassphrase", parsed.host)
    }
}
