package com.goldenv2.core.network.parser

import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Serializes a [Server] back into a share URI (the inverse of the
 * ConfigParser implementations): vmess://, vless://, trojan://, ss://,
 * hysteria2://, ssh:// etc. so a config can be copied to the clipboard,
 * shared with other apps, or encoded as a QR code.
 */
object ServerUriSerializer {

    fun toUri(server: Server): String {
        return when (server.protocol) {
            Protocol.VMess -> vmessUri(server)
            Protocol.VLESS, Protocol.Trojan -> userInfoUri(server)
            Protocol.Shadowsocks -> shadowsocksUri(server)
            Protocol.Hysteria2 -> hysteria2Uri(server)
            Protocol.Hysteria -> hysteria1Uri(server)
            Protocol.Tuic -> tuicUri(server)
            Protocol.WireGuard -> wireguardUri(server)
            Protocol.Ssh -> sshUri(server)
            else -> unsupported(server)
        }
    }

    private fun unsupported(server: Server): String {
        throw UnsupportedOperationException(
            "Sharing ${server.protocol.name} configs is not supported yet"
        )
    }

    private fun encodeFragment(text: String): String =
        URLEncoder.encode(text, StandardCharsets.UTF_8.name())

    private fun encodeQueryParams(params: List<Pair<String, String?>>): String =
        params.filter { it.second != null }
            .joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, StandardCharsets.UTF_8.name())}=" +
                    URLEncoder.encode(value!!, StandardCharsets.UTF_8.name())
            }

    private fun hostPort(server: Server): String {
        // Bracket IPv6 addresses, mirroring extractHostPort()
        val host = if (server.address.contains(':')) "[${server.address}]" else server.address
        return "$host:${server.port}"
    }

    private fun vmessUri(server: Server): String {
        val vmessJson = buildJsonObject {
            put("v", "2")
            put("ps", server.name)
            put("add", server.address)
            put("port", server.port.toString())
            put("id", server.uuid.orEmpty())
            put("aid", server.alterId.toString())
            put("scy", server.security)
            put("net", server.network.name)
            server.sni?.let { put("sni", it) }
            server.host?.let { put("host", it) }
            server.path?.let { put("path", it) }
            server.fingerprint?.let { put("fp", it) }
            put("type", server.headerType ?: "none")
            put("tls", if (server.tls) "tls" else "")
            server.alpn.takeIf { it.isNotEmpty() }?.let { put("alpn", it.joinToString(",")) }
        }
        val json = Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), vmessJson)
        val base64 = Base64.getEncoder().withoutPadding().encodeToString(json.toByteArray())
        return "vmess://$base64"
    }

    /**
     * vless://uuid@host:port?params#name and trojan://password@host:port?params#name
     */
    private fun userInfoUri(server: Server): String {
        val scheme = server.protocol.name.lowercase()
        val userInfo = when (server.protocol) {
            Protocol.VLESS -> server.uuid.orEmpty()
            else -> server.password.orEmpty()
        }
        val params = listOf(
            "type" to server.network.name.takeIf { it != "tcp" },
            "security" to when {
                server.publicKey != null -> "reality"
                server.tls -> "tls"
                else -> null
            },
            "sni" to server.sni,
            "alpn" to server.alpn.takeIf { it.isNotEmpty() }?.joinToString(","),
            "fp" to server.fingerprint,
            "flow" to server.flow,
            "pbk" to server.publicKey,
            "sid" to server.shortId,
            "spx" to server.spiderX,
            "path" to server.path,
            "host" to server.host,
            "headerType" to server.headerType
        ).filter { it.second != null }

        val queryString = encodeQueryParams(params)
        val query = if (queryString.isNotEmpty()) "?$queryString" else ""
        val fragment = encodeFragment(server.name)
        return "$scheme://${userInfo}@${hostPort(server)}$query#$fragment"
    }

    private fun shadowsocksUri(server: Server): String {
        // ss://base64(method:password)@host:port#name
        val methodAndPassword = server.password.orEmpty()
        val base64 = Base64.getEncoder().withoutPadding()
            .encodeToString(methodAndPassword.toByteArray())
        return "ss://$base64@${hostPort(server)}#${encodeFragment(server.name)}"
    }

    private fun hysteria2Uri(server: Server): String {
        val params = listOf(
            "sni" to (server.sni ?: server.address),
            "insecure" to "false",
            "alpn" to server.alpn.takeIf { it.isNotEmpty() }?.joinToString(","),
            "obfs" to server.obfs,
            "obfs-password" to server.obfsParam
        )
        val queryString = encodeQueryParams(params)
        val query = if (queryString.isNotEmpty()) "?$queryString" else ""
        val password = server.password.orEmpty()
        return "hysteria2://$password@${hostPort(server)}$query#${encodeFragment(server.name)}"
    }

    private fun hysteria1Uri(server: Server): String {
        // hysteria://password@host:port?protocol=&obfs=&auth_param=&peer=&insecure=0#name
        val params = listOf(
            "protocol" to server.network.name.takeIf { it != "tcp" },
            "peer" to server.sni,
            "obfs" to server.obfs,
            "auth_param" to server.obfsParam,
            "insecure" to if (server.tls) "0" else "1"
        )
        val queryString = encodeQueryParams(params.filter { it.second != null })
        val query = if (queryString.isNotEmpty()) "?$queryString" else ""
        val password = server.password.orEmpty()
        return "hysteria://$password@${hostPort(server)}$query#${encodeFragment(server.name)}"
    }

    private fun tuicUri(server: Server): String {
        // tuic5://uuid:password@host:port?sni=&#name
        val params = listOf(
            "sni" to server.sni,
            "token" to server.password
        )
        val queryString = encodeQueryParams(params.filter { it.second != null })
        val query = if (queryString.isNotEmpty()) "?$queryString" else ""
        val userInfo = listOfNotNull(server.uuid, server.password).joinToString(":")
        return "tuic5://$userInfo@${hostPort(server)}$query#${encodeFragment(server.name)}"
    }

    private fun wireguardUri(server: Server): String {
        // wg://privatekey:address@host:port?publickey=&allowedips=&label=
        val privateKey = server.uuid.orEmpty()
        val address = server.path ?: "10.0.0.2/32"
        val params = listOf(
            "publickey" to server.publicKey,
            "allowedips" to (server.host ?: "0.0.0.0/0"),
            "label" to server.name
        )
        val queryString = encodeQueryParams(params.filter { it.second != null })
        val query = if (queryString.isNotEmpty()) "?$queryString" else ""
        return "wg://$privateKey:$address@${hostPort(server)}$query"
    }

    private fun sshUri(server: Server): String {
        val userInfo = if (!server.password.isNullOrBlank()) {
            "${server.uuid.orEmpty()}:${server.password}"
        } else {
            server.uuid.orEmpty()
        }
        val params = listOf(
            "keyfile" to server.path,
            "keypass" to server.host
        )
        val queryString = encodeQueryParams(params)
        val query = if (queryString.isNotEmpty()) "?$queryString" else ""
        return "ssh://$userInfo@${hostPort(server)}$query#${encodeFragment(server.name)}"
    }
}
