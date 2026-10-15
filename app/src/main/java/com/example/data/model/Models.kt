package com.example.data.model

enum class CommunicationChannel(
    val label: String,
    val identifier: String
) {
    WHATSAPP("WhatsApp", "WHATSAPP"),
    TELEGRAM("Telegram (@username)", "TELEGRAM")
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
    val countryCode: String,
    val channel: CommunicationChannel,
    val contactDetail: String,
    val uid: String,
    val token: String,
    val approvalStatus: ApprovalStatus,
    val quotaTotalBytes: Long,
    val quotaUsedBytes: Long,
    val expirationTimestamp: Long,
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
    val connectedDurationSeconds: Long = 0
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

data class DynamicServerConfig(
    val host: String? = null,
    val port: Int = 443,
    val uuid: String? = null,
    val transport: String = "ws",
    val path: String = "/",
    val sni: String? = null,
    val security: String = "tls",
    val rawConfigJson: String? = null
)

data class VpnPreferences(
    val dnsPrimary: String = "1.1.1.1",
    val dnsSecondary: String = "8.8.8.8",
    val mtu: Int = 1500,
    val bypassLan: Boolean = true,
    val killSwitchEnabled: Boolean = false,
    val autoConnectOnBoot: Boolean = false,
    val showNotificationStats: Boolean = true
)
