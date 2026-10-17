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
                val uuid = if (json.has("uuid")) json.optString("uuid") else if (json.has("uid")) json.optString("uid") else null
                val wsPath = json.optString("wsPath", json.optString("path", "/nexustunnel"))
                val sni = json.optString("sni", "crashlyticsreports-pa.googleapis.com")
                val headerHost = json.optString("headerHost", json.optString("hostHeader", "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio"))
                val enableTls = json.optBoolean("enableTls", json.optBoolean("tls", true))
                val enableMux = json.optBoolean("enableMux", json.optBoolean("mux", true))
                val vlessUri = if (json.has("vlessUri")) json.optString("vlessUri") else null

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

    /**
     * Génère la configuration standard JSON officielle pour Xray-Core.
     * Inbounds: SOCKS 127.0.0.1:10808 + HTTP 127.0.0.1:10809
     * Outbounds: VLESS avec wsSettings (path, Host), tlsSettings (serverName, allowInsecure) et mux.
     */
    fun generateXrayJsonConfig(config: DynamicServerConfig): String {
        val host = config.serverAddress?.takeIf { it.isNotBlank() } ?: "192.179.18.100"
        val port = if (config.serverPort > 0) config.serverPort else 443
        val uuid = config.uuid?.takeIf { it.isNotBlank() } ?: "83d5a4b2-a45b-48c1-9012-3456789abcde"
        val rawPath = if (config.wsPath.isNotBlank()) config.wsPath else "/nexustunnel"
        val path = if (rawPath.startsWith("/")) rawPath else "/$rawPath"
        val sni = config.sni?.takeIf { it.isNotBlank() } ?: "crashlyticsreports-pa.googleapis.com"
        val hostHeader = config.headerHost?.takeIf { it.isNotBlank() } ?: "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio"
        val isTls = config.enableTls
        val isMux = config.enableMux

        val root = JSONObject()

        // Logs
        val logObj = JSONObject()
        logObj.put("loglevel", "warning")
        root.put("log", logObj)

        // Inbounds
        val inbounds = org.json.JSONArray()
        val socksInbound = JSONObject().apply {
            put("tag", "socks-in")
            put("port", 10808)
            put("listen", "127.0.0.1")
            put("protocol", "socks")
            put("settings", JSONObject().apply {
                put("auth", "noauth")
                put("udp", true)
            })
            put("sniffing", JSONObject().apply {
                put("enabled", true)
                put("destOverride", org.json.JSONArray().put("http").put("tls"))
            })
        }
        inbounds.put(socksInbound)

        val httpInbound = JSONObject().apply {
            put("tag", "http-in")
            put("port", 10809)
            put("listen", "127.0.0.1")
            put("protocol", "http")
            put("settings", JSONObject().apply {
                put("allowTransparent", false)
            })
        }
        inbounds.put(httpInbound)
        root.put("inbounds", inbounds)

        // Outbounds
        val outbounds = org.json.JSONArray()
        val vlessOutbound = JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")

            val settings = JSONObject()
            val vnext = org.json.JSONArray()
            val serverObj = JSONObject().apply {
                put("address", host)
                put("port", port)
                val users = org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", uuid)
                        put("encryption", "none")
                        put("level", 0)
                    })
                }
                put("users", users)
            }
            vnext.put(serverObj)
            settings.put("vnext", vnext)
            put("settings", settings)

            val streamSettings = JSONObject().apply {
                put("network", "ws")
                put("security", if (isTls) "tls" else "none")
                if (isTls) {
                    put("tlsSettings", JSONObject().apply {
                        put("serverName", sni)
                        put("allowInsecure", true)
                    })
                }
                put("wsSettings", JSONObject().apply {
                    put("path", path)
                    put("headers", JSONObject().apply {
                        put("Host", hostHeader)
                    })
                })
            }
            put("streamSettings", streamSettings)

            put("mux", JSONObject().apply {
                put("enabled", isMux)
                put("concurrency", 8)
            })
        }
        outbounds.put(vlessOutbound)

        val directOutbound = JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
            put("settings", JSONObject())
        }
        outbounds.put(directOutbound)
        root.put("outbounds", outbounds)

        return root.toString(2)
    }
}

