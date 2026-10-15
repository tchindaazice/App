package com.example.data.model

enum class CommunicationChannel(
    val label: String,
    val iconEmoji: String,
    val placeholder: String
) {
    WHATSAPP("Numéro WhatsApp", "💬", "Ex: 6XXXXXXXX ou +237 6XXXXXXXX"),
    TELEGRAM("Telegram (@username)", "✈️", "Ex: @mon_pseudo_telegram")
}

enum class ApprovalStatus {
    NOT_SUBMITTED,
    PENDING,
    APPROVED,
    REJECTED
}

data class DeviceInfo(
    val deviceId: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int
)

data class RegistrationRequest(
    val fullName: String,
    val country: Country,
    val channel: CommunicationChannel,
    val contactDetail: String,
    val deviceInfo: DeviceInfo,
    val requestedAt: Long = System.currentTimeMillis()
)

data class UserSession(
    val fullName: String,
    val countryName: String,
    val countryFlag: String,
    val channel: CommunicationChannel,
    val contactDetail: String,
    val uid: String,
    val token: String,
    val approvalStatus: ApprovalStatus,
    val quotaTotalBytes: Long,      // e.g. 50 GB
    val quotaUsedBytes: Long,       // consumed
    val expirationTimestamp: Long,  // date d'expiration
    val registeredDeviceId: String,
    val registeredAt: Long
)

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

data class ConnectionStats(
    val uploadSpeedBps: Long = 0,
    val downloadSpeedBps: Long = 0,
    val pingMs: Long = 0,
    val sessionBytesUp: Long = 0,
    val sessionBytesDown: Long = 0,
    val connectedDurationSeconds: Long = 0,
    val serverIp: String = "104.21.72.18",
    val protocolName: String = "VLESS-XTLS / WebSocket"
)

data class LogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val level: LogLevel,
    val message: String
)

enum class LogLevel {
    INFO, SUCCESS, WARN, ERROR
}

data class VpnConfig(
    val webPanelUrl: String = "https://nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio",
    val transport: String = "WebSocket + TLS",
    val serverPort: Int = 443,
    val customSni: String = "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio",
    val dnsPrimary: String = "1.1.1.1",
    val dnsSecondary: String = "8.8.8.8",
    val mtu: Int = 1500,
    val bypassLan: Boolean = true,
    val killSwitchEnabled: Boolean = false,
    val enableMux: Boolean = true
)
