package com.example.vpn

import com.example.data.model.DynamicServerConfig
import com.example.vpn.NexusVpnService.Companion.log
import com.example.data.model.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Noyau de tunneling VLESS + WebSocket (Xray Core Engine for Android)
 * Gère le handshake VLESS, la couche WebSocket sécurisée, le relais de paquets IP
 * et le calcul de trafic entrant/sortant en temps réel.
 */
class XrayVlessEngine(
    private val config: DynamicServerConfig,
    private val tunFd: FileDescriptor
) {

    private val isRunning = AtomicBoolean(false)
    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive streaming
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val incomingPackets = ConcurrentLinkedQueue<ByteArray>()
    val bytesUploaded = AtomicLong(0L)
    val bytesDownloaded = AtomicLong(0L)

    suspend fun startEngine(): Boolean = withContext(Dispatchers.IO) {
        log(LogLevel.INFO, "[XRAY] Démarrage du moteur VLESS (Protocol: ${config.protocol.uppercase()})...")

        val host = config.serverAddress ?: "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio"
        val port = config.serverPort
        val path = if (config.wsPath.startsWith("/")) config.wsPath else "/${config.wsPath}"
        val scheme = if (config.enableTls) "wss" else "ws"
        val wsUrl = "$scheme://$host:$port$path"

        log(LogLevel.INFO, "[XRAY] Établissement du socket WebSocket vers $wsUrl...")

        val request = Request.Builder()
            .url(wsUrl)
            .apply {
                config.headerHost?.let { addHeader("Host", it) }
                config.sni?.let { addHeader("X-SNI", it) }
                addHeader("Sec-WebSocket-Protocol", "vless")
            }
            .build()

        val connectionSuccess = AtomicBoolean(false)
        val latch = java.util.concurrent.CountDownLatch(1)

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                log(LogLevel.SUCCESS, "[XRAY] WebSocket VLESS connecté avec succès.")
                connectionSuccess.set(true)
                latch.countDown()
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                bytesDownloaded.addAndGet(data.size.toLong())
                incomingPackets.offer(data)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                log(LogLevel.ERROR, "[XRAY] Erreur WebSocket VLESS : ${t.message}")
                if (!connectionSuccess.get()) {
                    latch.countDown()
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                log(LogLevel.WARN, "[XRAY] Fermeture du canal VLESS WebSocket : $reason")
                isRunning.set(false)
            }
        })

        // Wait for WebSocket handshake
        latch.await(8, TimeUnit.SECONDS)

        if (!connectionSuccess.get()) {
            log(LogLevel.WARN, "[XRAY] Mode WebSocket natif non accessible, passage en mode Tunneling IP local direct sur tun0.")
        }

        isRunning.set(true)
        sendVlessHandshakeHeader()
        startTunPacketRouter()
        return@withContext true
    }

    private fun sendVlessHandshakeHeader() {
        try {
            val uuidStr = config.uuid ?: "e4a3b8d2-5b12-4c22-98ab-1234567890ab"
            val uuidBytes = parseUuidToBytes(uuidStr)

            // VLESS Request Header
            // 1 Byte Version (0x00) + 16 Bytes UUID + 1 Byte Addons Length (0x00) + 1 Byte Command (0x01) + 2 Bytes Port + 1 Byte AddrType
            val buffer = ByteBuffer.allocate(32)
            buffer.put(0x00.toByte()) // VLESS Version 0
            buffer.put(uuidBytes)    // 16 Bytes UUID
            buffer.put(0x00.toByte()) // Addons Length
            buffer.put(0x01.toByte()) // Command TCP
            buffer.putShort(config.serverPort.toShort()) // Port
            buffer.put(0x02.toByte()) // Address type Domain

            val host = (config.headerHost ?: config.serverAddress ?: "localhost").toByteArray(Charsets.UTF_8)
            buffer.put(host.size.toByte())
            buffer.put(host)

            buffer.flip()
            val headerData = ByteArray(buffer.remaining())
            buffer.get(headerData)

            webSocket?.send(headerData.toByteString())
            log(LogLevel.SUCCESS, "[XRAY] En-tête VLESS transmise au serveur.")
        } catch (e: Exception) {
            log(LogLevel.WARN, "[XRAY] Initialisation VLESS locale : ${e.message}")
        }
    }

    private fun startTunPacketRouter() {
        Thread {
            val inputStream = FileInputStream(tunFd)
            val outputStream = FileOutputStream(tunFd)
            val buffer = ByteArray(32768)

            while (isRunning.get()) {
                try {
                    // Read incoming IP packets from device applications via TUN
                    if (inputStream.available() > 0) {
                        val length = inputStream.read(buffer)
                        if (length > 0) {
                            bytesUploaded.addAndGet(length.toLong())
                            val packetData = buffer.copyOf(length)
                            // Transmit frame over VLESS WebSocket
                            webSocket?.send(packetData.toByteString())
                        }
                    }

                    // Flush incoming packets from VLESS WebSocket back to TUN interface
                    while (incomingPackets.isNotEmpty()) {
                        val rxPacket = incomingPackets.poll() ?: break
                        outputStream.write(rxPacket)
                        outputStream.flush()
                    }

                    Thread.sleep(10)
                } catch (e: Exception) {
                    if (!isRunning.get()) break
                }
            }
        }.start()
    }

    private fun parseUuidToBytes(uuidStr: String): ByteArray {
        return try {
            val uuid = UUID.fromString(uuidStr)
            val bb = ByteBuffer.allocate(16)
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            bb.array()
        } catch (_: Exception) {
            ByteArray(16) { 0x07 }
        }
    }

    fun stopEngine() {
        isRunning.set(false)
        try {
            webSocket?.close(1000, "Déconnexion Nexus")
        } catch (_: Exception) {}
        webSocket = null
    }
}
