package com.example.vpn

import android.net.Uri
import com.example.data.model.DynamicServerConfig
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Analyseur et formateur universel de configurations et liens VLESS (vless://).
 * Compatible avec les liens générés par le Web Panel, dark tunnel, v2rayNG et Xray-core.
 */
object VlessConfigParser {

    fun parse(rawInput: String): DynamicServerConfig? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        // 1. Essai d'analyse en tant que JSON brut
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = JSONObject(trimmed)
                val protocol = json.optString("protocol", "vless")
                val serverAddress = json.optString("serverAddress", json.optString("host", "192.179.18.100"))
                val serverPort = json.optInt("serverPort", json.optInt("port", 443))
                val uuid = json.optString("uuid", json.optString("uid", null))
                val wsPath = json.optString("wsPath", json.optString("path", "/nexustunnel"))
                val sni = json.optString("sni", "crashlyticsreports-pa.googleapis.com")
                val headerHost = json.optString("headerHost", json.optString("hostHeader", "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio"))
                val enableTls = json.optBoolean("enableTls", json.optBoolean("tls", true))
                val enableMux = json.optBoolean("enableMux", json.optBoolean("mux", true))
                val vlessUri = json.optString("vlessUri", null)

                return DynamicServerConfig(
                    protocol = protocol,
                    serverAddress = serverAddress,
                    serverPort = serverPort,
                    uuid = uuid,
                    wsPath = wsPath,
                    sni = sni,
                    headerHost = headerHost,
                    enableTls = enableTls,
                    enableMux = enableMux,
                    vlessUri = vlessUri,
                    rawConfigJson = trimmed
                )
            } catch (_: Exception) {
                // Continuer vers l'analyse URI
            }
        }

        // 2. Essai d'analyse en tant qu'URI VLESS : vless://uuid@host:port?query#remarks
        if (trimmed.startsWith("vless://", ignoreCase = true)) {
            try {
                val uriString = trimmed
                val withoutScheme = uriString.substring(8)
                val atIndex = withoutScheme.indexOf('@')
                if (atIndex == -1) return null

                val uuid = withoutScheme.substring(0, atIndex).trim()
                val rest = withoutScheme.substring(atIndex + 1)

                val questionIndex = rest.indexOf('?')
                val hashIndex = rest.indexOf('#')

                val hostPortStr = when {
                    questionIndex != -1 -> rest.substring(0, questionIndex)
                    hashIndex != -1 -> rest.substring(0, hashIndex)
                    else -> rest
                }

                val host: String
                val port: Int
                if (hostPortStr.contains(':')) {
                    val parts = hostPortStr.split(':')
                    host = parts[0]
                    port = parts.getOrNull(1)?.toIntOrNull() ?: 443
                } else {
                    host = hostPortStr
                    port = 443
                }

                // Query params
                var wsPath = "/nexustunnel"
                var sni = "crashlyticsreports-pa.googleapis.com"
                var headerHost = "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio"
                var enableTls = true
                var enableMux = true

                if (questionIndex != -1) {
                    val queryEnd = if (hashIndex != -1 && hashIndex > questionIndex) hashIndex else rest.length
                    val queryString = rest.substring(questionIndex + 1, queryEnd)
                    val pairs = queryString.split('&')
                    for (pair in pairs) {
                        val kv = pair.split('=')
                        if (kv.isNotEmpty()) {
                            val key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name())
                            val value = if (kv.size > 1) URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()) else ""
                            when (key.lowercase()) {
                                "path" -> if (value.isNotBlank()) wsPath = value
                                "sni" -> if (value.isNotBlank()) sni = value
                                "host" -> if (value.isNotBlank()) headerHost = value
                                "security" -> enableTls = value.equals("tls", ignoreCase = true)
                                "mux" -> enableMux = value.equals("true", ignoreCase = true) || value == "1"
                            }
                        }
                    }
                }

                return DynamicServerConfig(
                    protocol = "vless",
                    serverAddress = host,
                    serverPort = port,
                    uuid = uuid,
                    wsPath = wsPath,
                    sni = sni,
                    headerHost = headerHost,
                    enableTls = enableTls,
                    enableMux = enableMux,
                    vlessUri = trimmed,
                    rawConfigJson = null
                )
            } catch (_: Exception) {
                return null
            }
        }

        return null
    }

    fun toVlessUri(config: DynamicServerConfig, remarks: String = "Nexus VLESS"): String {
        val host = config.serverAddress ?: "192.179.18.100"
        val port = config.serverPort
        val uuid = config.uuid ?: "default_uuid"
        val path = try { URLEncoder.encode(config.wsPath, StandardCharsets.UTF_8.name()) } catch (_: Exception) { config.wsPath }
        val sni = try { URLEncoder.encode(config.sni ?: host, StandardCharsets.UTF_8.name()) } catch (_: Exception) { config.sni ?: host }
        val headerHost = try { URLEncoder.encode(config.headerHost ?: host, StandardCharsets.UTF_8.name()) } catch (_: Exception) { config.headerHost ?: host }
        val sec = if (config.enableTls) "tls" else "none"
        val rem = try { URLEncoder.encode(remarks, StandardCharsets.UTF_8.name()) } catch (_: Exception) { remarks }

        return "vless://$uuid@$host:$port?type=ws&security=$sec&path=$path&host=$headerHost&sni=$sni#$rem"
    }
}
