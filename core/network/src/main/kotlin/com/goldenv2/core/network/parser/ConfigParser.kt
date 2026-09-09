package com.goldenv2.core.network.parser

import com.goldenv2.core.domain.model.NetworkType
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

interface ConfigParser {
    fun parse(uri: String): ParseResult
    fun getSupportedSchemes(): List<String>
}

sealed class ParseResult {
    data class Success(val server: Server) : ParseResult()
    data class Failure(val error: String) : ParseResult()
}

abstract class BaseParser : ConfigParser {
    internal fun decodeBase64(input: String): String {
        return try {
            val decoded = Base64.getDecoder().decode(input)
            String(decoded, StandardCharsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            // Try URL-safe base64
            val padded = input.padEnd(input.length + (4 - input.length % 4) % 4, '=')
                .replace('-', '+').replace('_', '/')
            val decoded = Base64.getDecoder().decode(padded)
            String(decoded, StandardCharsets.UTF_8)
        }
    }

    protected fun decodeBase64UrlSafe(input: String): String {
        val padded = input.padEnd(input.length + (4 - input.length % 4) % 4, '=')
            .replace('-', '+').replace('_', '/')
        return String(Base64.getDecoder().decode(padded), StandardCharsets.UTF_8)
    }

    protected fun parseQueryParams(uri: String): Map<String, String> {
        val queryStart = uri.indexOf('?')
        if (queryStart == -1) return emptyMap()
        val query = uri.substring(queryStart + 1)
        return query.split('&')
            .map { it.split('=', limit = 2) }
            .associate { (key, value) ->
                URLDecoder.decode(key, StandardCharsets.UTF_8.name()) to
                    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
            }
    }

    protected fun parseFragment(uri: String): String? {
        val fragmentStart = uri.indexOf('#')
        if (fragmentStart == -1) return null
        return URLDecoder.decode(uri.substring(fragmentStart + 1), StandardCharsets.UTF_8.name())
    }

    protected fun extractHostPort(hostPort: String): Pair<String, Int> {
        val bracketEnd = hostPort.indexOf(']')
        if (hostPort.startsWith("[") && bracketEnd != -1) {
            // IPv6 address
            val host = hostPort.substring(1, bracketEnd)
            val portStr = hostPort.substring(bracketEnd + 1).removePrefix(":")
            return host to (portStr.toIntOrNull() ?: 443)
        }
        val colonIndex = hostPort.lastIndexOf(':')
        if (colonIndex != -1) {
            val host = hostPort.substring(0, colonIndex)
            val port = hostPort.substring(colonIndex + 1).toIntOrNull() ?: 443
            return host to port
        }
        return hostPort to 443
    }

    protected fun generateId(): String = UUID.randomUUID().toString()
}

class VmParser : BaseParser() {
    override fun getSupportedSchemes() = listOf("vmess")

    override fun parse(uri: String): ParseResult {
        return try {
            val base64Part = uri.removePrefix("vmess://")
            val jsonStr = decodeBase64(base64Part)
            val json = Json.parseToJsonElement(jsonStr).jsonObject

            val id = generateId()
            val name = json["ps"]?.jsonPrimitive?.contentOrNull ?: "VMess Server"
            val address = json["add"]?.jsonPrimitive?.contentOrNull ?: ""
            val port = json["port"]?.jsonPrimitive?.intOrNull ?: 443
            val uuid = json["id"]?.jsonPrimitive?.contentOrNull ?: ""
            val alterId = json["aid"]?.jsonPrimitive?.intOrNull ?: 0
            val security = json["scy"]?.jsonPrimitive?.contentOrNull ?: "auto"
            val network = NetworkType.valueOf(json["net"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: "tcp")
            val tls = json["tls"]?.jsonPrimitive?.contentOrNull?.lowercase() == "tls"
            val sni = json["sni"]?.jsonPrimitive?.contentOrNull
            val alpn = json["alpn"]?.jsonPrimitive?.contentOrNull?.split(",") ?: emptyList()
            val fingerprint = json["fp"]?.jsonPrimitive?.contentOrNull
            val path = json["path"]?.jsonPrimitive?.contentOrNull
            val host = json["host"]?.jsonPrimitive?.contentOrNull
            val headerType = json["type"]?.jsonPrimitive?.contentOrNull
            val group = parseFragment(uri)

            val server = Server(
                id = id,
                subscriptionId = "",
                name = name,
                protocol = Protocol.VMess,
                address = address,
                port = port,
                uuid = uuid,
                alterId = alterId,
                security = security,
                network = network,
                tls = tls,
                sni = sni,
                alpn = alpn,
                fingerprint = fingerprint,
                path = path,
                host = host,
                headerType = headerType,
                group = group
            )
            ParseResult.Success(server)
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse VMess: ${e.message}")
        }
    }
}

class VlessParser : BaseParser() {
    override fun getSupportedSchemes() = listOf("vless")

    override fun parse(uri: String): ParseResult {
        return try {
            // vless://uuid@host:port?params#name
            val withoutScheme = uri.removePrefix("vless://")
            val hashIndex = withoutScheme.indexOf('#')
            val mainPart = if (hashIndex != -1) withoutScheme.substring(0, hashIndex) else withoutScheme
            val name = if (hashIndex != -1) URLDecoder.decode(withoutScheme.substring(hashIndex + 1), StandardCharsets.UTF_8.name()) else "VLESS Server"

            val atIndex = mainPart.indexOf('@')
            if (atIndex == -1) return ParseResult.Failure("Invalid VLESS format: missing @")

            val uuid = mainPart.substring(0, atIndex)
            val hostPortQuery = mainPart.substring(atIndex + 1)

            val queryStart = hostPortQuery.indexOf('?')
            val hostPort = if (queryStart != -1) hostPortQuery.substring(0, queryStart) else hostPortQuery
            val query = if (queryStart != -1) hostPortQuery.substring(queryStart) else ""

            val (address, port) = extractHostPort(hostPort)
            val params = parseQueryParams("?" + query)

            val id = generateId()
            val network = NetworkType.valueOf(params["type"]?.lowercase() ?: "tcp")
            val security = params["security"]?.lowercase() ?: "none"
            val tls = security == "tls" || security == "reality"
            val sni = params["sni"]
            val alpn = params["alpn"]?.split(",") ?: emptyList()
            val fingerprint = params["fp"]
            val flow = params["flow"]
            val path = params["path"]
            val host = params["host"]
            val headerType = params["headerType"]
            val publicKey = params["pbk"]
            val shortId = params["sid"]
            val spiderX = params["spx"]

            val server = Server(
                id = id,
                subscriptionId = "",
                name = name,
                protocol = Protocol.VLESS,
                address = address,
                port = port,
                uuid = uuid,
                network = network,
                tls = tls,
                sni = sni,
                alpn = alpn,
                fingerprint = fingerprint,
                path = path,
                host = host,
                headerType = headerType,
                flow = flow,
                publicKey = publicKey,
                shortId = shortId,
                spiderX = spiderX,
                group = parseFragment(uri)
            )
            ParseResult.Success(server)
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse VLESS: ${e.message}")
        }
    }
}

class TrojanParser : BaseParser() {
    override fun getSupportedSchemes() = listOf("trojan")

    override fun parse(uri: String): ParseResult {
        return try {
            // trojan://password@host:port?params#name
            val withoutScheme = uri.removePrefix("trojan://")
            val hashIndex = withoutScheme.indexOf('#')
            val mainPart = if (hashIndex != -1) withoutScheme.substring(0, hashIndex) else withoutScheme
            val name = if (hashIndex != -1) URLDecoder.decode(withoutScheme.substring(hashIndex + 1), StandardCharsets.UTF_8.name()) else "Trojan Server"

            val atIndex = mainPart.indexOf('@')
            if (atIndex == -1) return ParseResult.Failure("Invalid Trojan format: missing @")

            val password = mainPart.substring(0, atIndex)
            val hostPortQuery = mainPart.substring(atIndex + 1)

            val queryStart = hostPortQuery.indexOf('?')
            val hostPort = if (queryStart != -1) hostPortQuery.substring(0, queryStart) else hostPortQuery
            val query = if (queryStart != -1) hostPortQuery.substring(queryStart) else ""

            val (address, port) = extractHostPort(hostPort)
            val params = parseQueryParams("?" + query)

            val id = generateId()
            val network = NetworkType.valueOf(params["type"]?.lowercase() ?: "tcp")
            val security = params["security"]?.lowercase() ?: "tls"
            val tls = security == "tls"
            val sni = params["sni"]
            val alpn = params["alpn"]?.split(",") ?: emptyList()
            val fingerprint = params["fp"]
            val path = params["path"]
            val host = params["host"]
            val headerType = params["headerType"]
            val allowInsecure = params["allowInsecure"]?.toBoolean() ?: false

            val server = Server(
                id = id,
                subscriptionId = "",
                name = name,
                protocol = Protocol.Trojan,
                address = address,
                port = port,
                password = password,
                network = network,
                tls = tls,
                sni = sni,
                alpn = alpn,
                fingerprint = fingerprint,
                path = path,
                host = host,
                headerType = headerType,
                group = parseFragment(uri)
            )
            ParseResult.Success(server)
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse Trojan: ${e.message}")
        }
    }
}

class ShadowsocksParser : BaseParser() {
    override fun getSupportedSchemes() = listOf("ss")

    override fun parse(uri: String): ParseResult {
        return try {
            // ss://method:password@host:port#name or ss://base64(method:password)@host:port#name
            val withoutScheme = uri.removePrefix("ss://")
            val hashIndex = withoutScheme.indexOf('#')
            val mainPart = if (hashIndex != -1) withoutScheme.substring(0, hashIndex) else withoutScheme
            val name = if (hashIndex != -1) URLDecoder.decode(withoutScheme.substring(hashIndex + 1), StandardCharsets.UTF_8.name()) else "Shadowsocks Server"

            val atIndex = mainPart.indexOf('@')
            if (atIndex == -1) {
                // Try base64 encoded full config
                val decoded = decodeBase64(mainPart)
                val atIndex2 = decoded.indexOf('@')
                if (atIndex2 != -1) {
                    val userInfo = decoded.substring(0, atIndex2)
                    val hostPort = decoded.substring(atIndex2 + 1)
                    val colonIndex = userInfo.indexOf(':')
                    if (colonIndex != -1) {
                        val method = userInfo.substring(0, colonIndex)
                        val password = userInfo.substring(colonIndex + 1)
                        val (address, port) = extractHostPort(hostPort)
                        val id = generateId()
                        val server = Server(
                            id = id,
                            subscriptionId = "",
                            name = name,
                            protocol = Protocol.Shadowsocks,
                            address = address,
                            port = port,
                            password = "$method:$password",
                            group = parseFragment(uri)
                        )
                        return ParseResult.Success(server)
                    }
                }
                return ParseResult.Failure("Invalid Shadowsocks format: missing @")
            }

            val userInfo = mainPart.substring(0, atIndex)
            val hostPort = mainPart.substring(atIndex + 1)

            // Check if userInfo is base64 encoded
            val decodedUserInfo = try {
                decodeBase64(userInfo)
            } catch (e: Exception) {
                userInfo
            }

            val colonIndex = decodedUserInfo.indexOf(':')
            if (colonIndex == -1) return ParseResult.Failure("Invalid Shadowsocks format: missing : in userinfo")

            val method = decodedUserInfo.substring(0, colonIndex)
            val password = decodedUserInfo.substring(colonIndex + 1)
            val (address, port) = extractHostPort(hostPort)

            val id = generateId()
            val server = Server(
                id = id,
                subscriptionId = "",
                name = name,
                protocol = Protocol.Shadowsocks,
                address = address,
                port = port,
                password = "$method:$password",
                group = parseFragment(uri)
            )
            ParseResult.Success(server)
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse Shadowsocks: ${e.message}")
        }
    }
}

class Hysteria2Parser : BaseParser() {
    override fun getSupportedSchemes() = listOf("hysteria2", "hy2")

    override fun parse(uri: String): ParseResult {
        return try {
            // hysteria2://password@host:port?params#name
            val withoutScheme = uri.removePrefix("hysteria2://").removePrefix("hy2://")
            val hashIndex = withoutScheme.indexOf('#')
            val mainPart = if (hashIndex != -1) withoutScheme.substring(0, hashIndex) else withoutScheme
            val name = if (hashIndex != -1) URLDecoder.decode(withoutScheme.substring(hashIndex + 1), StandardCharsets.UTF_8.name()) else "Hysteria2 Server"

            val atIndex = mainPart.indexOf('@')
            if (atIndex == -1) return ParseResult.Failure("Invalid Hysteria2 format: missing @")

            val password = mainPart.substring(0, atIndex)
            val hostPortQuery = mainPart.substring(atIndex + 1)

            val queryStart = hostPortQuery.indexOf('?')
            val hostPort = if (queryStart != -1) hostPortQuery.substring(0, queryStart) else hostPortQuery
            val query = if (queryStart != -1) hostPortQuery.substring(queryStart) else ""

            val (address, port) = extractHostPort(hostPort)
            val params = parseQueryParams("?" + query)

            val id = generateId()
            val sni = params["sni"] ?: address
            val insecure = params["insecure"]?.toBoolean() ?: false
            val alpn = params["alpn"]?.split(",") ?: listOf("h3")
            val obfs = params["obfs"]
            val obfsPassword = params["obfs-password"]

            val server = Server(
                id = id,
                subscriptionId = "",
                name = name,
                protocol = Protocol.Hysteria2,
                address = address,
                port = port,
                password = password,
                sni = sni,
                tls = true,
                alpn = alpn,
                obfs = obfs,
                obfsParam = obfsPassword,
                group = parseFragment(uri)
            )
            ParseResult.Success(server)
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse Hysteria2: ${e.message}")
        }
    }
}

class SshParser : BaseParser() {
    override fun getSupportedSchemes() = listOf("ssh")

    override fun parse(uri: String): ParseResult {
        return try {
            val withoutScheme = uri.removePrefix("ssh://")
            val hashIndex = withoutScheme.indexOf('#')
            val mainPart = if (hashIndex != -1) withoutScheme.substring(0, hashIndex) else withoutScheme
            val name = if (hashIndex != -1) URLDecoder.decode(withoutScheme.substring(hashIndex + 1), StandardCharsets.UTF_8.name()) else "SSH Server"

            val atIndex = mainPart.indexOf('@')
            if (atIndex == -1) return ParseResult.Failure("Invalid SSH format: missing @")

            val userInfo = mainPart.substring(0, atIndex)
            val hostPortQuery = mainPart.substring(atIndex + 1)

            val queryStart = hostPortQuery.indexOf('?')
            val hostPort = if (queryStart != -1) hostPortQuery.substring(0, queryStart) else hostPortQuery
            val query = if (queryStart != -1) hostPortQuery.substring(queryStart) else ""

            val (address, port) = extractHostPort(hostPort)
            val params = parseQueryParams("?" + query)

            val (username, password) = if (userInfo.contains(":")) {
                val parts = userInfo.split(":", limit = 2)
                parts[0] to parts[1]
            } else {
                userInfo to ""
            }

            val id = generateId()
            val keyFile = params["keyfile"]
            val keyPassphrase = params["keypass"]

            val server = Server(
                id = id,
                subscriptionId = "",
                name = name,
                protocol = Protocol.Ssh,
                address = address,
                port = port,
                uuid = username,
                password = password,
                path = keyFile,
                host = keyPassphrase,
                group = parseFragment(uri)
            )
            ParseResult.Success(server)
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse SSH: ${e.message}")
        }
    }
}

object ConfigParserRegistry {
    private val parsers: Map<String, ConfigParser> = mapOf(
        "vmess" to VmParser(),
        "vless" to VlessParser(),
        "trojan" to TrojanParser(),
        "ss" to ShadowsocksParser(),
        "hysteria2" to Hysteria2Parser(),
        "hy2" to Hysteria2Parser(),
        "ssh" to SshParser()
    )

    fun parse(uri: String): ParseResult {
        val schemeEnd = uri.indexOf("://")
        if (schemeEnd == -1) return ParseResult.Failure("Invalid URI format: missing scheme")

        val scheme = uri.substring(0, schemeEnd).lowercase()
        val parser = parsers[scheme]
            ?: return ParseResult.Failure("Unsupported protocol: $scheme")

        return parser.parse(uri)
    }

    fun parseAll(uris: List<String>): List<ParseResult> = uris.map { parse(it) }

    fun getSupportedSchemes(): List<String> = parsers.keys.toList()
}

fun parseSubscriptionContent(content: String): List<Server> {
    val decoded = try {
        VmParser().decodeBase64(content.trim())
    } catch (e: Exception) {
        content.trim()
    }

    return decoded.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { ConfigParserRegistry.parse(it) }
        .mapNotNull { if (it is ParseResult.Success) it.server else null }
}