package com.goldenv2.app

import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.network.parser.ConfigParserRegistry
import com.goldenv2.core.network.parser.ParseResult
import com.goldenv2.core.network.parser.ServerUriSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay of the QR-scanner path on the actual device. The scanner returns raw text,
 * which is fed directly into `onImportConfig` -> `ConfigParserRegistry.parse`.
 */
class SshQrImportTest {

    @Test
    fun scannedSshQrWithBase64UserinfoYieldsCorrectUsernameAndPassword() {
        // Exact payload scanned from the QR; userinfo is base64 of "root:uAuPZkJ1o_fSeTO0E"
        // with URL-encoded padding ("%3D%3D").
        val payload = "ssh://cm9vdDp1QXVQWmtKMW9fZlNlVE8wRQ%3D%3D@31.56.27.149:22#L%20ssh"
        val result = ConfigParserRegistry.parse(payload)

        assertTrue("parse failed: ${(result as? ParseResult.Failure)?.error}", result is ParseResult.Success)
        val server = (result as ParseResult.Success).server

        assertEquals(Protocol.Ssh, server.protocol)
        assertEquals("31.56.27.149", server.address)
        assertEquals(22, server.port)
        assertEquals("root", server.uuid)                 // not the base64 blob
        assertEquals("uAuPZkJ1o_fSeTO0E", server.password) // decoded password
        assertEquals("L ssh", server.name)                 // URL-decoded fragment
    }

    @Test
    fun sshUriRoundTripsSpecialCharactersInCredentials() {
        val server = Server(
            subscriptionId = "",
            name = "My SSH",
            protocol = Protocol.Ssh,
            address = "ssh.example.com",
            port = 22,
            uuid = "user@example.com",
            password = "p@ss:word#1"
        )
        val uri = ServerUriSerializer.toUri(server)
        val result = ConfigParserRegistry.parse(uri)

        assertTrue("parse failed: ${(result as? ParseResult.Failure)?.error}", result is ParseResult.Success)
        val parsed = (result as ParseResult.Success).server

        assertEquals("user@example.com", parsed.uuid)
        assertEquals("p@ss:word#1", parsed.password)
    }
}
