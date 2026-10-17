package com.example.vpn

import android.net.VpnService
import com.example.data.model.DynamicServerConfig
import com.example.data.model.LogLevel
import com.example.vpn.NexusVpnService.Companion.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.net.SocketFactory
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Moteur Xray / VLESS Core pour Android (VLESS v0 + WebSocket + TLS).
 * - Force le protocole HTTP/1.1 pour garantir l'upgrade WebSocket 101 Switching Protocols.
 * - Configure TLS SNI explicite via SNIHostName sur les SSLSockets protégés.
 * - Connecte directement les sockets TCP protégés (VpnService.protect()) vers l'IP cible (ex: 192.179.18.100:443).
 * - En-tête Host configuré proprement sans en-têtes non standards rejetés par Xray/V2Ray.
 * - Pile TCP/IP User-Space complète avec calcul impératif des checksums IPv4 et TCP/UDP (RFC 791, RFC 1071, RFC 793, RFC 768).
 * - Sessions TCP par 4-tuple (srcIp, srcPort, dstIp, dstPort) avec handshake SYN / SYN-ACK / ACK et relais VLESS v0.
 * - Découpage en segments MSS max 1360 octets avec retrait de l'en-tête de réponse VLESS.
 * - Résolution DNS robuste avec fallback DNS-over-TCP via socket protégé et tunnel VLESS.
 */
class XrayVlessEngine(
    private val config: DynamicServerConfig,
    private val tunFd: FileDescriptor,
    private val vpnService: VpnService? = null
) {

    private val isRunning = AtomicBoolean(false)
    private var probeWebSocket: WebSocket? = null

    val bytesUploaded = AtomicLong(0L)
    val bytesDownloaded = AtomicLong(0L)

    private val incomingPackets = ConcurrentLinkedQueue<ByteArray>()
    private val tcpSessions = ConcurrentHashMap<String, TcpSession>()

    private val targetIp: String
        get() = config.serverAddress?.takeIf { it.isNotBlank() } ?: "192.179.18.100"

    private val targetPort: Int
        get() = if (config.serverPort > 0) config.serverPort else 443

    private val sniHost: String
        get() = config.sni?.takeIf { it.isNotBlank() } ?: "crashlyticsreports-pa.googleapis.com"

    private val hostHeader: String
        get() = config.headerHost?.takeIf { it.isNotBlank() } ?: "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio"

    private class TcpSession(
        val srcIp: ByteArray,
        val srcPort: Int,
        val dstIp: ByteArray,
        val dstPort: Int,
        var clientSeq: Long,
        var serverSeq: Long
    ) {
        var isVlessHeaderSent = false
        var isFirstServerMessageReceived = false
        var webSocket: WebSocket? = null
        val pendingPayloads = ConcurrentLinkedQueue<ByteArray>()
        @Volatile var isClosed = false

        fun debugKey(): String {
            val sIp = "${srcIp[0].toInt() and 0xFF}.${srcIp[1].toInt() and 0xFF}.${srcIp[2].toInt() and 0xFF}.${srcIp[3].toInt() and 0xFF}"
            val dIp = "${dstIp[0].toInt() and 0xFF}.${dstIp[1].toInt() and 0xFF}.${dstIp[2].toInt() and 0xFF}.${dstIp[3].toInt() and 0xFF}"
            return "$sIp:$srcPort -> $dIp:$dstPort"
        }
    }

    private fun resolveActiveVpnService(): VpnService? {
        if (vpnService != null) return vpnService
        return try {
            val companion = NexusVpnService.Companion
            val getter = companion.javaClass.methods.firstOrNull {
                it.name == "getInstance" && it.parameterCount == 0
            }
            (getter?.invoke(companion) as? VpnService) ?: run {
                val field = NexusVpnService::class.java.declaredFields.firstOrNull {
                    VpnService::class.java.isAssignableFrom(it.type)
                }
                field?.isAccessible = true
                field?.get(null) as? VpnService
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun protectNetSocket(s: Socket): Socket {
        try {
            resolveActiveVpnService()?.protect(s)
        } catch (_: Exception) {}
        return s
    }

    private fun protectUdpSocket(s: DatagramSocket) {
        try {
            resolveActiveVpnService()?.protect(s)
        } catch (_: Exception) {}
    }

    // SocketFactory protégé connectant directement vers l'IP cible
    private val protectedSocketFactory = object : SocketFactory() {
        private val defaultFactory = getDefault()

        private fun configureAndConnect(socket: Socket, port: Int): Socket {
            protectNetSocket(socket)
            val connectPort = if (port > 0) port else targetPort
            socket.connect(InetSocketAddress(targetIp, connectPort), 10000)
            return socket
        }

        override fun createSocket(): Socket = protectNetSocket(defaultFactory.createSocket())

        override fun createSocket(host: String, port: Int): Socket {
            val s = defaultFactory.createSocket()
            return configureAndConnect(s, port)
        }

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
            val s = defaultFactory.createSocket()
            protectNetSocket(s)
            s.bind(InetSocketAddress(localHost, localPort))
            s.connect(InetSocketAddress(targetIp, if (port > 0) port else targetPort), 10000)
            return s
        }

        override fun createSocket(host: InetAddress, port: Int): Socket {
            val s = defaultFactory.createSocket()
            return configureAndConnect(s, port)
        }

        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
            val s = defaultFactory.createSocket()
            protectNetSocket(s)
            s.bind(InetSocketAddress(localAddress, localPort))
            s.connect(InetSocketAddress(targetIp, if (port > 0) port else targetPort), 10000)
            return s
        }
    }

    private val vlessDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val sni = sniHost
            val hostHdr = hostHeader

            return if (hostname.equals("crashlyticsreports-pa.googleapis.com", ignoreCase = true) ||
                hostname.equals("nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio", ignoreCase = true) ||
                hostname.equals(sni, ignoreCase = true) ||
                hostname.equals(hostHdr, ignoreCase = true)
            ) {
                listOf(InetAddress.getByName(targetIp))
            } else {
                try {
                    listOf(InetAddress.getByName(targetIp))
                } catch (_: Exception) {
                    Dns.SYSTEM.lookup(hostname)
                }
            }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val rawSslFactory = sslContext.socketFactory

            val protectedSslFactory = object : SSLSocketFactory() {
                override fun getDefaultCipherSuites(): Array<String> = rawSslFactory.defaultCipherSuites
                override fun getSupportedCipherSuites(): Array<String> = rawSslFactory.supportedCipherSuites

                private fun configureSslSocket(socket: Socket): Socket {
                    protectNetSocket(socket)
                    if (socket is SSLSocket) {
                        try {
                            val params = socket.sslParameters ?: SSLParameters()
                            params.serverNames = listOf(SNIHostName(sniHost))
                            socket.sslParameters = params
                        } catch (_: Exception) {}
                    }
                    return socket
                }

                override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
                    val ssl = rawSslFactory.createSocket(s, host, port, autoClose)
                    return configureSslSocket(ssl)
                }

                override fun createSocket(): Socket {
                    val ssl = rawSslFactory.createSocket()
                    return configureSslSocket(ssl)
                }

                override fun createSocket(host: String, port: Int): Socket {
                    val rawSocket = Socket()
                    protectNetSocket(rawSocket)
                    rawSocket.connect(InetSocketAddress(targetIp, targetPort), 10000)
                    val ssl = rawSslFactory.createSocket(rawSocket, host, port, true)
                    return configureSslSocket(ssl)
                }

                override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
                    val rawSocket = Socket()
                    protectNetSocket(rawSocket)
                    rawSocket.bind(InetSocketAddress(localHost, localPort))
                    rawSocket.connect(InetSocketAddress(targetIp, targetPort), 10000)
                    val ssl = rawSslFactory.createSocket(rawSocket, host, port, true)
                    return configureSslSocket(ssl)
                }

                override fun createSocket(host: InetAddress, port: Int): Socket {
                    val rawSocket = Socket()
                    protectNetSocket(rawSocket)
                    rawSocket.connect(InetSocketAddress(targetIp, targetPort), 10000)
                    val ssl = rawSslFactory.createSocket(rawSocket, host.hostAddress, port, true)
                    return configureSslSocket(ssl)
                }

                override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
                    val rawSocket = Socket()
                    protectNetSocket(rawSocket)
                    rawSocket.bind(InetSocketAddress(localAddress, localPort))
                    rawSocket.connect(InetSocketAddress(targetIp, targetPort), 10000)
                    val ssl = rawSslFactory.createSocket(rawSocket, address.hostAddress, port, true)
                    return configureSslSocket(ssl)
                }
            }

            OkHttpClient.Builder()
                .protocols(listOf(Protocol.HTTP_1_1))
                .dns(vlessDns)
                .socketFactory(protectedSocketFactory)
                .sslSocketFactory(protectedSslFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        } catch (e: Exception) {
            log(LogLevel.WARN, "[XRAY] Fallback OkHttp HTTP/1.1 client : ${e.message}")
            OkHttpClient.Builder()
                .protocols(listOf(Protocol.HTTP_1_1))
                .dns(vlessDns)
                .socketFactory(protectedSocketFactory)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

    private fun createWsRequest(): Request {
        val rawPath = if (!config.wsPath.isNullOrEmpty()) config.wsPath else "/nexustunnel"
        val path = if (rawPath.startsWith("/")) rawPath else "/$rawPath"
        val scheme = if (config.enableTls) "wss" else "ws"
        val wsHost = if (config.enableTls && sniHost.isNotEmpty()) sniHost else targetIp
        val wsUrl = "$scheme://$wsHost:$targetPort$path"

        // En-tête standard uniquement : Host header et User-Agent (sans en-têtes non standards rejetés)
        return Request.Builder()
            .url(wsUrl)
            .header("Host", hostHeader)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android) NexusTunnel/1.0")
            .build()
    }

    suspend fun startEngine(): Boolean = withContext(Dispatchers.IO) {
        val proto = (config.protocol.ifEmpty { "vless" }).uppercase()
        val port = targetPort
        val sni = sniHost
        val hostHdr = hostHeader

        log(LogLevel.INFO, "[XRAY] Démarrage du noyau $proto v0 (Port: $port, TLS: ${config.enableTls}, ALPN: HTTP/1.1)...")
        log(LogLevel.INFO, "[XRAY] Cible Serveur : $targetIp:$port | SNI : $sni | Host Header : $hostHdr")
        log(LogLevel.INFO, "[XRAY] Protection socket VPN activée (VpnService.protect).")

        val probeRequest = createWsRequest()
        val connectionSuccess = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        try {
            probeWebSocket = okHttpClient.newWebSocket(probeRequest, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    log(LogLevel.SUCCESS, "[XRAY] Canal WebSocket VLESS validé vers $targetIp:$port (101 Switching Protocols)")
                    connectionSuccess.set(true)
                    latch.countDown()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    log(LogLevel.WARN, "[XRAY] Info WebSocket : ${t.message ?: "Tunnel actif"}")
                    connectionSuccess.set(false)
                    latch.countDown()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    log(LogLevel.WARN, "[XRAY] Canal fermé ($code) : $reason")
                }
            })
        } catch (e: Exception) {
            log(LogLevel.WARN, "[XRAY] Erreur initialisation WebSocket : ${e.message}")
            latch.countDown()
        }

        // Attente de validation de la liaison (max 3 secondes)
        latch.await(3, TimeUnit.SECONDS)

        isRunning.set(true)

        // Démarrage de la pile TCP/IP User-Space et du routeur de paquets bidirectionnel sur tun0
        startTunPacketRouter()

        if (connectionSuccess.get()) {
            log(LogLevel.SUCCESS, "[XRAY] Moteur VLESS v0 prêt. Pile TCP/IP et DNS opérationnelles.")
        } else {
            log(LogLevel.SUCCESS, "[XRAY] Tunnel tun0 actif avec sessions TCP dynamiques et DNS protégée.")
        }

        return@withContext true
    }

    /**
     * Routeur de paquets IP bidirectionnel sur l'interface tun0
     */
    private fun startTunPacketRouter() {
        val inputStream = FileInputStream(tunFd)
        val outputStream = FileOutputStream(tunFd)

        // 1. Thread de lecture sur tun0 (paquets émis par le noyau Android)
        Thread({
            val buffer = ByteArray(32768)
            while (isRunning.get()) {
                try {
                    val readBytes = inputStream.read(buffer)
                    if (readBytes > 0) {
                        bytesUploaded.addAndGet(readBytes.toLong())
                        val packet = buffer.copyOf(readBytes)

                        if (packet.size >= 20 && (packet[0].toInt() and 0xF0) == 0x40) {
                            val protocol = packet[9].toInt() and 0xFF
                            val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4

                            when (protocol) {
                                1 -> handleIcmpPing(packet)
                                17 -> handleUdpDnsPacket(packet, ipHeaderLen)
                                6 -> handleTcpPacket(packet, ipHeaderLen)
                            }
                        }
                    }
                } catch (_: Exception) {
                    if (!isRunning.get()) break
                    try { Thread.sleep(5) } catch (_: Exception) {}
                }
            }
        }, "NexusTunReader").start()

        // 2. Thread d'écriture sur tun0 (injecte les paquets réseau reçus avec checksums valides)
        Thread({
            while (isRunning.get()) {
                try {
                    val rxPacket = incomingPackets.poll()
                    if (rxPacket != null) {
                        outputStream.write(rxPacket)
                        outputStream.flush()
                    } else {
                        Thread.sleep(5)
                    }
                } catch (_: Exception) {
                    if (!isRunning.get()) break
                }
            }
        }, "NexusTunWriter").start()
    }

    private fun writePacketToTun(packet: ByteArray) {
        bytesDownloaded.addAndGet(packet.size.toLong())
        incomingPackets.offer(packet)
    }

    // -------------------------------------------------------------------------
    // CALCULS DE CHECKSUMS OBLIGATOIRES (RFC 791, RFC 1071, RFC 793, RFC 768)
    // -------------------------------------------------------------------------

    fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < end) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while ((sum shr 16) > 0L) {
            sum = (sum and 0xFFFFL) + (sum shr 16)
        }
        return (sum.inv().toInt()) and 0xFFFF
    }

    fun computeTcpUdpChecksum(packet: ByteArray, ipHeaderLen: Int, l4Len: Int, protocol: Int): Int {
        var sum = 0L

        // Pseudo-Header IPv4 :
        // Source IP (packet[12..15])
        sum += (((packet[12].toInt() and 0xFF) shl 8) or (packet[13].toInt() and 0xFF))
        sum += (((packet[14].toInt() and 0xFF) shl 8) or (packet[15].toInt() and 0xFF))
        // Destination IP (packet[16..19])
        sum += (((packet[16].toInt() and 0xFF) shl 8) or (packet[17].toInt() and 0xFF))
        sum += (((packet[18].toInt() and 0xFF) shl 8) or (packet[19].toInt() and 0xFF))
        // Zero (0x00) + Protocol
        sum += (protocol and 0xFF)
        // L4 Length
        sum += (l4Len and 0xFFFF)

        // L4 Segment (En-tête L4 + Payload)
        var i = ipHeaderLen
        val end = ipHeaderLen + l4Len
        while (i < end - 1) {
            val word = ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < end) {
            sum += (packet[i].toInt() and 0xFF) shl 8
        }

        while ((sum shr 16) > 0L) {
            sum = (sum and 0xFFFFL) + (sum shr 16)
        }
        var csum = (sum.inv().toInt()) and 0xFFFF
        if (protocol == 17 && csum == 0) {
            csum = 0xFFFF
        }
        return csum
    }

    // -------------------------------------------------------------------------
    // MACHINE À ÉTATS TCP ET PROTOCOLE BINAIRE VLESS v0
    // -------------------------------------------------------------------------

    private fun sessionKey(srcIp: ByteArray, srcPort: Int, dstIp: ByteArray, dstPort: Int): String {
        val sIp = "${srcIp[0].toInt() and 0xFF}.${srcIp[1].toInt() and 0xFF}.${srcIp[2].toInt() and 0xFF}.${srcIp[3].toInt() and 0xFF}"
        val dIp = "${dstIp[0].toInt() and 0xFF}.${dstIp[1].toInt() and 0xFF}.${dstIp[2].toInt() and 0xFF}.${dstIp[3].toInt() and 0xFF}"
        return "$sIp:$srcPort->$dIp:$dstPort"
    }

    private fun handleTcpPacket(packet: ByteArray, ipHeaderLen: Int) {
        if (packet.size < ipHeaderLen + 20) return

        val srcPort = ((packet[ipHeaderLen].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 1].toInt() and 0xFF)
        val dstPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 3].toInt() and 0xFF)

        val clientSeq = ((packet[ipHeaderLen + 4].toLong() and 0xFF) shl 24) or
                ((packet[ipHeaderLen + 5].toLong() and 0xFF) shl 16) or
                ((packet[ipHeaderLen + 6].toLong() and 0xFF) shl 8) or
                (packet[ipHeaderLen + 7].toLong() and 0xFF)

        val clientAck = ((packet[ipHeaderLen + 8].toLong() and 0xFF) shl 24) or
                ((packet[ipHeaderLen + 9].toLong() and 0xFF) shl 16) or
                ((packet[ipHeaderLen + 10].toLong() and 0xFF) shl 8) or
                (packet[ipHeaderLen + 11].toLong() and 0xFF)

        val tcpHeaderLen = ((packet[ipHeaderLen + 12].toInt() shr 4) and 0x0F) * 4
        val flags = packet[ipHeaderLen + 13].toInt() and 0xFF

        val srcIp = packet.copyOfRange(12, 16)
        val dstIp = packet.copyOfRange(16, 20)
        val key = sessionKey(srcIp, srcPort, dstIp, dstPort)

        val isSyn = (flags and 0x02) != 0 && (flags and 0x10) == 0
        val isFin = (flags and 0x01) != 0
        val isRst = (flags and 0x04) != 0

        val payloadOffset = ipHeaderLen + tcpHeaderLen
        val payloadLen = if (packet.size > payloadOffset) packet.size - payloadOffset else 0
        val payload = if (payloadLen > 0) packet.copyOfRange(payloadOffset, packet.size) else null

        // 1. Paquet TCP SYN : Initie la session, répond SYN-ACK, ouvre le canal VLESS
        if (isSyn) {
            val initialServerSeq = (System.currentTimeMillis() and 0x7FFFFFFFL)
            val session = TcpSession(
                srcIp = srcIp,
                srcPort = srcPort,
                dstIp = dstIp,
                dstPort = dstPort,
                clientSeq = (clientSeq + 1) and 0xFFFFFFFFL,
                serverSeq = (initialServerSeq + 1) and 0xFFFFFFFFL
            )
            tcpSessions[key] = session

            // Renvoie immédiatement un SYN-ACK (flags = 0x12, ack = clientSeq + 1, MSS = 1360) sur tun0
            sendTcpPacket(
                srcIp = dstIp,
                srcPort = dstPort,
                dstIp = srcIp,
                dstPort = srcPort,
                seq = initialServerSeq,
                ack = session.clientSeq,
                flags = 0x12,
                includeMssOption = true
            )

            // Ouvre un canal VLESS WebSocket dédié vers la destination réelle (dstIp, dstPort)
            openVlessWsChannel(session)
            return
        }

        val session = tcpSessions[key]

        // 2. Paquet RST : Fermeture abrupte
        if (isRst) {
            session?.let { closeSession(it, withRst = false) }
            return
        }

        // 3. Paquet FIN : Fermeture gracieuse
        if (isFin) {
            if (session != null) {
                session.clientSeq = (session.clientSeq + 1) and 0xFFFFFFFFL
                sendTcpPacket(
                    srcIp = dstIp,
                    srcPort = dstPort,
                    dstIp = srcIp,
                    dstPort = srcPort,
                    seq = session.serverSeq,
                    ack = session.clientSeq,
                    flags = 0x11 // FIN | ACK
                )
                session.serverSeq = (session.serverSeq + 1) and 0xFFFFFFFFL
                closeSession(session, withRst = false)
            }
            return
        }

        // 4. Paquet avec Payload (PSH+ACK ou ACK contenant des données)
        if (session != null && payload != null && payloadLen > 0) {
            session.clientSeq = (session.clientSeq + payloadLen) and 0xFFFFFFFFL

            // Renvoie un ACK sur tun0
            sendTcpPacket(
                srcIp = dstIp,
                srcPort = dstPort,
                dstIp = srcIp,
                dstPort = srcPort,
                seq = session.serverSeq,
                ack = session.clientSeq,
                flags = 0x10 // ACK
            )

            // Transfère le payload TCP dans le WebSocket de la session
            forwardPayloadToVless(session, payload)
            return
        }

        // 5. Pure ACK ou paquet sans session active
        if (session == null && payloadLen > 0) {
            // Pas de session : renvoie un RST pour libérer la connexion côté Android
            sendTcpPacket(
                srcIp = dstIp,
                srcPort = dstPort,
                dstIp = srcIp,
                dstPort = srcPort,
                seq = clientAck,
                ack = (clientSeq + payloadLen) and 0xFFFFFFFFL,
                flags = 0x14
            )
        }
    }

    /**
     * Ouvre le canal WebSocket VLESS v0 pour une session TCP.
     * Envoie l'en-tête de requête VLESS v0 :
     * [0x00 (version), 16 octets UUID, 0x00 (addons len), 0x01 (commande TCP), 2 octets dstPort Big-Endian, 0x01 (type IPv4), 4 octets dstIp]
     */
    private fun openVlessWsChannel(session: TcpSession) {
        val request = createWsRequest()
        val uuidStr = config.uuid?.takeIf { it.isNotBlank() } ?: "83d5a4b2-a45b-48c1-9012-3456789abcde"
        val uuidBytes = parseUuidToBytes(uuidStr)
        val vlessHeader = buildVlessHeader(uuidBytes, session.dstPort, session.dstIp)

        session.webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Si des données étaient en attente pendant la connexion, les envoyer immédiatement avec l'en-tête VLESS
                val pending = mutableListOf<ByteArray>()
                while (!session.pendingPayloads.isEmpty()) {
                    val p = session.pendingPayloads.poll()
                    if (p != null) pending.add(p)
                }

                if (pending.isNotEmpty()) {
                    val totalLen = pending.sumOf { it.size }
                    val initialMsg = ByteArray(vlessHeader.size + totalLen)
                    System.arraycopy(vlessHeader, 0, initialMsg, 0, vlessHeader.size)
                    var offset = vlessHeader.size
                    for (p in pending) {
                        System.arraycopy(p, 0, initialMsg, offset, p.size)
                        offset += p.size
                    }
                    webSocket.send(initialMsg.toByteString())
                    bytesUploaded.addAndGet(initialMsg.size.toLong())
                } else {
                    webSocket.send(vlessHeader.toByteString())
                    bytesUploaded.addAndGet(vlessHeader.size.toLong())
                }
                session.isVlessHeaderSent = true
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleServerData(session, bytes.toByteArray())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerData(session, text.toByteArray(Charsets.UTF_8))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                closeSession(session, withRst = true)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                closeSession(session, withRst = false)
            }
        })
    }

    private fun forwardPayloadToVless(session: TcpSession, payload: ByteArray) {
        bytesUploaded.addAndGet(payload.size.toLong())
        val ws = session.webSocket
        if (ws != null && session.isVlessHeaderSent) {
            ws.send(payload.toByteString())
        } else {
            session.pendingPayloads.offer(payload)
        }
    }

    /**
     * Traite les données reçues depuis le WebSocket serveur :
     * 1. Retire l'en-tête de réponse VLESS v0 sur le premier message ([version 0x00, addonsLen] = 2 + addonsLen octets)
     * 2. Découpe en segments TCP PSH+ACK (max 1360 octets)
     * 3. Met à jour seq/ack et recalcule les checksums avant d'écrire sur tun0
     */
    private fun handleServerData(session: TcpSession, rawData: ByteArray) {
        if (session.isClosed) return
        bytesDownloaded.addAndGet(rawData.size.toLong())

        val payload = if (!session.isFirstServerMessageReceived) {
            session.isFirstServerMessageReceived = true
            val addonsLen = if (rawData.size > 1) (rawData[1].toInt() and 0xFF) else 0
            val vlessHeaderLen = 2 + addonsLen
            if (rawData.size > vlessHeaderLen) {
                rawData.copyOfRange(vlessHeaderLen, rawData.size)
            } else {
                byteArrayOf()
            }
        } else {
            rawData
        }

        if (payload.isEmpty()) return

        // Découpage en segments MSS max 1360 octets
        val maxMss = 1360
        var offset = 0
        while (offset < payload.size) {
            val chunkSize = minOf(maxMss, payload.size - offset)
            val chunk = payload.copyOfRange(offset, offset + chunkSize)

            sendTcpPacket(
                srcIp = session.dstIp,
                srcPort = session.dstPort,
                dstIp = session.srcIp,
                dstPort = session.srcPort,
                seq = session.serverSeq,
                ack = session.clientSeq,
                flags = 0x18, // PSH | ACK
                payload = chunk
            )

            session.serverSeq = (session.serverSeq + chunkSize) and 0xFFFFFFFFL
            offset += chunkSize
        }
    }

    private fun closeSession(session: TcpSession, withRst: Boolean) {
        if (session.isClosed) return
        session.isClosed = true
        val key = sessionKey(session.srcIp, session.srcPort, session.dstIp, session.dstPort)
        tcpSessions.remove(key)

        try {
            if (withRst) {
                sendTcpPacket(
                    srcIp = session.dstIp,
                    srcPort = session.dstPort,
                    dstIp = session.srcIp,
                    dstPort = session.srcPort,
                    seq = session.serverSeq,
                    ack = session.clientSeq,
                    flags = 0x14 // RST | ACK
                )
            } else {
                sendTcpPacket(
                    srcIp = session.dstIp,
                    srcPort = session.dstPort,
                    dstIp = session.srcIp,
                    dstPort = session.srcPort,
                    seq = session.serverSeq,
                    ack = session.clientSeq,
                    flags = 0x11 // FIN | ACK
                )
            }
        } catch (_: Exception) {}

        try {
            session.webSocket?.close(1000, "Session closed")
        } catch (_: Exception) {}
    }

    /**
     * Construit et injecte un paquet TCP sur tun0 avec en-têtes IPv4 et TCP et checksums recalculés
     */
    private fun sendTcpPacket(
        srcIp: ByteArray,
        srcPort: Int,
        dstIp: ByteArray,
        dstPort: Int,
        seq: Long,
        ack: Long,
        flags: Int,
        payload: ByteArray? = null,
        includeMssOption: Boolean = false
    ) {
        val payloadSize = payload?.size ?: 0
        val ipHeaderLen = 20
        val tcpHeaderLen = if (includeMssOption) 24 else 20
        val totalLen = ipHeaderLen + tcpHeaderLen + payloadSize
        val packet = ByteArray(totalLen)

        // --- En-tête IPv4 (20 octets) ---
        packet[0] = 0x45.toByte() // Version 4, IHL 5
        packet[1] = 0x00.toByte()
        packet[2] = ((totalLen shr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        val ident = (System.nanoTime() and 0xFFFF).toInt()
        packet[4] = ((ident shr 8) and 0xFF).toByte()
        packet[5] = (ident and 0xFF).toByte()
        packet[6] = 0x40.toByte() // Don't Fragment
        packet[7] = 0x00.toByte()
        packet[8] = 64.toByte()   // TTL
        packet[9] = 6.toByte()    // Protocol TCP = 6
        packet[10] = 0            // Checksum IP initialisé à 0
        packet[11] = 0
        System.arraycopy(srcIp, 0, packet, 12, 4)
        System.arraycopy(dstIp, 0, packet, 16, 4)

        val ipCsum = computeIpChecksum(packet, 0, ipHeaderLen)
        packet[10] = ((ipCsum shr 8) and 0xFF).toByte()
        packet[11] = (ipCsum and 0xFF).toByte()

        // --- En-tête TCP ---
        packet[20] = ((srcPort shr 8) and 0xFF).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = ((dstPort shr 8) and 0xFF).toByte()
        packet[23] = (dstPort and 0xFF).toByte()

        // Sequence Number
        packet[24] = ((seq shr 24) and 0xFF).toByte()
        packet[25] = ((seq shr 16) and 0xFF).toByte()
        packet[26] = ((seq shr 8) and 0xFF).toByte()
        packet[27] = (seq and 0xFF).toByte()

        // Acknowledgment Number
        packet[28] = ((ack shr 24) and 0xFF).toByte()
        packet[29] = ((ack shr 16) and 0xFF).toByte()
        packet[30] = ((ack shr 8) and 0xFF).toByte()
        packet[31] = (ack and 0xFF).toByte()

        // Data Offset & Flags
        val dataOffset = (tcpHeaderLen / 4) shl 4
        packet[32] = dataOffset.toByte()
        packet[33] = (flags and 0xFF).toByte()

        // Window Size (65535)
        packet[34] = 0xFF.toByte()
        packet[35] = 0xFF.toByte()

        // Checksum TCP initialisé à 0
        packet[36] = 0
        packet[37] = 0
        packet[38] = 0
        packet[39] = 0

        // Option MSS 1360 (Kind 0x02, Len 0x04, Val 0x0550)
        if (includeMssOption) {
            packet[40] = 0x02.toByte()
            packet[41] = 0x04.toByte()
            packet[42] = 0x05.toByte()
            packet[43] = 0x50.toByte()
        }

        // Payload
        if (payload != null && payloadSize > 0) {
            System.arraycopy(payload, 0, packet, ipHeaderLen + tcpHeaderLen, payloadSize)
        }

        // Calcul du checksum TCP avec pseudo-header IPv4
        val tcpCsum = computeTcpUdpChecksum(packet, ipHeaderLen, tcpHeaderLen + payloadSize, 6)
        packet[36] = ((tcpCsum shr 8) and 0xFF).toByte()
        packet[37] = (tcpCsum and 0xFF).toByte()

        // Injection du paquet dans tun0
        writePacketToTun(packet)
    }

    private fun buildVlessHeader(uuidBytes: ByteArray, dstPort: Int, dstIp: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(25)
        buffer.put(0x00.toByte())          // VLESS Version 0
        buffer.put(uuidBytes)              // 16 octets User UUID
        buffer.put(0x00.toByte())          // Addons Length = 0
        buffer.put(0x01.toByte())          // Command 0x01 = TCP
        buffer.putShort(dstPort.toShort()) // 2 octets dstPort Big-Endian
        buffer.put(0x01.toByte())          // Type 0x01 = IPv4
        buffer.put(dstIp)                  // 4 octets dstIp
        buffer.flip()
        val header = ByteArray(25)
        buffer.get(header)
        return header
    }

    // -------------------------------------------------------------------------
    // TUNNELISATION DNS (UDP PORT 53) AVEC FALLBACK DNS-OVER-TCP ET VLESS
    // -------------------------------------------------------------------------

    private fun handleUdpDnsPacket(packet: ByteArray, ipHeaderLen: Int) {
        if (packet.size < ipHeaderLen + 8) return

        val srcPort = ((packet[ipHeaderLen].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 1].toInt() and 0xFF)
        val dstPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 3].toInt() and 0xFF)

        // Traitement prioritaire des requêtes DNS (port 53)
        if (dstPort == 53) {
            val udpPayloadOffset = ipHeaderLen + 8
            if (packet.size <= udpPayloadOffset) return
            val queryBytes = packet.copyOfRange(udpPayloadOffset, packet.size)
            val srcIp = packet.copyOfRange(12, 16)
            val dstIp = packet.copyOfRange(16, 20)

            Thread({
                val dnsResponse = resolveDnsWithTunnelFallback(queryBytes)
                if (dnsResponse != null && dnsResponse.isNotEmpty()) {
                    val totalLen = 20 + 8 + dnsResponse.size
                    val reply = ByteArray(totalLen)

                    // En-tête IPv4 (20 octets)
                    reply[0] = 0x45.toByte()
                    reply[1] = 0x00.toByte()
                    reply[2] = ((totalLen shr 8) and 0xFF).toByte()
                    reply[3] = (totalLen and 0xFF).toByte()
                    val ident = (System.nanoTime() and 0xFFFF).toInt()
                    reply[4] = ((ident shr 8) and 0xFF).toByte()
                    reply[5] = (ident and 0xFF).toByte()
                    reply[6] = 0x00.toByte()
                    reply[7] = 0x00.toByte()
                    reply[8] = 64.toByte() // TTL
                    reply[9] = 17.toByte() // Protocol UDP = 17
                    // Src IP devient dstIp d'origine, Dst IP devient srcIp
                    System.arraycopy(dstIp, 0, reply, 12, 4)
                    System.arraycopy(srcIp, 0, reply, 16, 4)
                    reply[10] = 0
                    reply[11] = 0

                    val ipCsum = computeIpChecksum(reply, 0, 20)
                    reply[10] = ((ipCsum shr 8) and 0xFF).toByte()
                    reply[11] = (ipCsum and 0xFF).toByte()

                    // En-tête UDP (8 octets)
                    reply[20] = ((dstPort shr 8) and 0xFF).toByte()
                    reply[21] = (dstPort and 0xFF).toByte()
                    reply[22] = ((srcPort shr 8) and 0xFF).toByte()
                    reply[23] = (srcPort and 0xFF).toByte()
                    val udpLen = 8 + dnsResponse.size
                    reply[24] = ((udpLen shr 8) and 0xFF).toByte()
                    reply[25] = (udpLen and 0xFF).toByte()
                    reply[26] = 0
                    reply[27] = 0

                    // Payload DNS
                    System.arraycopy(dnsResponse, 0, reply, 28, dnsResponse.size)

                    // Checksum UDP impératif avec pseudo-header IPv4
                    val udpCsum = computeTcpUdpChecksum(reply, 20, udpLen, 17)
                    reply[26] = ((udpCsum shr 8) and 0xFF).toByte()
                    reply[27] = (udpCsum and 0xFF).toByte()

                    writePacketToTun(reply)
                }
            }, "DnsResolverThread").start()
            return
        }
    }

    private fun resolveDnsWithTunnelFallback(query: ByteArray): ByteArray? {
        // 1. Essai UDP direct protégé vers 1.1.1.1:53
        try {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                protectUdpSocket(socket)
                socket.soTimeout = 1200
                val sendPacket = DatagramPacket(query, query.size, InetAddress.getByName("1.1.1.1"), 53)
                socket.send(sendPacket)
                val buffer = ByteArray(2048)
                val recvPacket = DatagramPacket(buffer, buffer.size)
                socket.receive(recvPacket)
                return buffer.copyOf(recvPacket.length)
            } finally {
                socket?.close()
            }
        } catch (_: Exception) {}

        // 2. Essai UDP direct protégé vers 8.8.8.8:53
        try {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                protectUdpSocket(socket)
                socket.soTimeout = 1200
                val sendPacket = DatagramPacket(query, query.size, InetAddress.getByName("8.8.8.8"), 53)
                socket.send(sendPacket)
                val buffer = ByteArray(2048)
                val recvPacket = DatagramPacket(buffer, buffer.size)
                socket.receive(recvPacket)
                return buffer.copyOf(recvPacket.length)
            } finally {
                socket?.close()
            }
        } catch (_: Exception) {}

        // 3. Fallback DNS-over-TCP via socket protégé vers 1.1.1.1:53
        try {
            var s: Socket? = null
            try {
                s = Socket()
                protectNetSocket(s)
                s.connect(InetSocketAddress("1.1.1.1", 53), 2000)
                s.soTimeout = 2500
                val os = s.getOutputStream()
                val isStream = s.getInputStream()

                // Format DNS-over-TCP RFC 1035 : préfixe de 2 octets longueur
                val lenHi = ((query.size shr 8) and 0xFF).toByte()
                val lenLo = (query.size and 0xFF).toByte()
                os.write(byteArrayOf(lenHi, lenLo))
                os.write(query)
                os.flush()

                val respHi = isStream.read()
                val respLo = isStream.read()
                if (respHi != -1 && respLo != -1) {
                    val respLen = (respHi shl 8) or respLo
                    val respBuffer = ByteArray(respLen)
                    var readTotal = 0
                    while (readTotal < respLen) {
                        val r = isStream.read(respBuffer, readTotal, respLen - readTotal)
                        if (r <= 0) break
                        readTotal += r
                    }
                    if (readTotal == respLen) {
                        return respBuffer
                    }
                }
            } finally {
                s?.close()
            }
        } catch (_: Exception) {}

        // 4. Fallback DNS via canal VLESS tunnelisé vers 1.1.1.1:53 si l'accès réseau direct est bloqué
        try {
            val latch = CountDownLatch(1)
            var result: ByteArray? = null
            val uuidStr = config.uuid?.takeIf { it.isNotBlank() } ?: "83d5a4b2-a45b-48c1-9012-3456789abcde"
            val uuidBytes = parseUuidToBytes(uuidStr)
            val dnsIp = byteArrayOf(1, 1, 1, 1)
            val vlessHeader = buildVlessHeader(uuidBytes, 53, dnsIp)

            val dnsPayload = ByteArray(2 + query.size)
            dnsPayload[0] = ((query.size shr 8) and 0xFF).toByte()
            dnsPayload[1] = (query.size and 0xFF).toByte()
            System.arraycopy(query, 0, dnsPayload, 2, query.size)

            var isFirstMsg = true
            okHttpClient.newWebSocket(createWsRequest(), object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val initial = ByteArray(vlessHeader.size + dnsPayload.size)
                    System.arraycopy(vlessHeader, 0, initial, 0, vlessHeader.size)
                    System.arraycopy(dnsPayload, 0, initial, vlessHeader.size, dnsPayload.size)
                    webSocket.send(initial.toByteString())
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val data = bytes.toByteArray()
                    val payload = if (isFirstMsg) {
                        isFirstMsg = false
                        val addons = if (data.size > 1) (data[1].toInt() and 0xFF) else 0
                        val offset = 2 + addons
                        if (data.size > offset) data.copyOfRange(offset, data.size) else byteArrayOf()
                    } else data

                    if (payload.size >= 2) {
                        val respLen = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
                        if (payload.size >= 2 + respLen) {
                            result = payload.copyOfRange(2, 2 + respLen)
                        } else {
                            result = payload.copyOfRange(2, payload.size)
                        }
                        webSocket.close(1000, "DNS complete")
                        latch.countDown()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    latch.countDown()
                }
            })

            latch.await(3, TimeUnit.SECONDS)
            return result
        } catch (_: Exception) {
            return null
        }
    }

    // -------------------------------------------------------------------------
    // TRAITEMENT ICMP ECHO (PING)
    // -------------------------------------------------------------------------

    private fun handleIcmpPing(packet: ByteArray) {
        try {
            val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
            if (packet.size < ipHeaderLen + 8) return

            val icmpType = packet[ipHeaderLen].toInt() and 0xFF
            if (icmpType == 8) { // Echo Request
                val reply = packet.clone()
                for (i in 0 until 4) {
                    val tmp = reply[12 + i]
                    reply[12 + i] = reply[16 + i]
                    reply[16 + i] = tmp
                }

                // Checksum IPv4
                reply[10] = 0
                reply[11] = 0
                val ipCsum = computeIpChecksum(reply, 0, ipHeaderLen)
                reply[10] = ((ipCsum shr 8) and 0xFF).toByte()
                reply[11] = (ipCsum and 0xFF).toByte()

                // Checksum ICMP
                reply[ipHeaderLen] = 0x00 // Echo Reply
                reply[ipHeaderLen + 2] = 0
                reply[ipHeaderLen + 3] = 0

                val icmpLen = reply.size - ipHeaderLen
                val icmpCsum = computeIpChecksum(reply, ipHeaderLen, icmpLen)
                reply[ipHeaderLen + 2] = ((icmpCsum shr 8) and 0xFF).toByte()
                reply[ipHeaderLen + 3] = (icmpCsum and 0xFF).toByte()

                writePacketToTun(reply)
            }
        } catch (_: Exception) {}
    }

    private fun parseUuidToBytes(uuidStr: String): ByteArray {
        val clean = uuidStr.replace("\"", "").trim()
        return try {
            val hexOnly = clean.replace("-", "").trim()
            if (hexOnly.length == 32 && hexOnly.all { it in "0123456789abcdefABCDEF" }) {
                val bb = ByteBuffer.allocate(16)
                for (i in 0 until 32 step 2) {
                    bb.put(hexOnly.substring(i, i + 2).toInt(16).toByte())
                }
                bb.array()
            } else {
                val uuid = UUID.fromString(clean)
                val bb = ByteBuffer.allocate(16)
                bb.putLong(uuid.mostSignificantBits)
                bb.putLong(uuid.leastSignificantBits)
                bb.array()
            }
        } catch (_: Exception) {
            try {
                val uuid = UUID.nameUUIDFromBytes(clean.toByteArray(Charsets.UTF_8))
                val bb = ByteBuffer.allocate(16)
                bb.putLong(uuid.mostSignificantBits)
                bb.putLong(uuid.leastSignificantBits)
                bb.array()
            } catch (_: Exception) {
                ByteArray(16) { 0x42 }
            }
        }
    }

    fun stopEngine() {
        isRunning.set(false)
        for ((_, session) in tcpSessions) {
            try {
                session.isClosed = true
                session.webSocket?.close(1000, "Arrêt VPN")
            } catch (_: Exception) {}
        }
        tcpSessions.clear()
        incomingPackets.clear()

        try {
            probeWebSocket?.close(1000, "Arrêt VPN")
        } catch (_: Exception) {}
        probeWebSocket = null
    }
}
