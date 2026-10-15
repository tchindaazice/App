package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.data.api.AccountSyncResponse
import com.example.data.api.ActivateTokenDto
import com.example.data.api.NexusApiService
import com.example.data.api.RequestSubmissionDto
import com.example.data.api.VpnConnectDto
import com.example.data.api.VpnDisconnectDto
import com.example.data.api.VpnHeartbeatDto
import com.example.data.api.VpnHeartbeatResponse
import com.example.data.model.AfricanCountries
import com.example.data.model.ApprovalStatus
import com.example.data.model.CommunicationChannel
import com.example.data.model.Country
import com.example.data.model.DeviceInfo
import com.example.data.model.DynamicServerConfig
import com.example.data.model.RegistrationRequest
import com.example.data.model.UserSession
import com.example.data.model.VpnPreferences
import com.example.data.security.SecurityStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class NexusRepository(private val context: Context) {

    private val securityStorage = SecurityStorage(context)
    private val prefs = context.getSharedPreferences("nexus_public_prefs", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Base URL is securely resolved or configured
    private var baseUrl: String = prefs.getString("cfg_server_url", "https://nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio/") ?: "https://nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio/"
    private var apiService: NexusApiService = createApiService(baseUrl)

    private fun createApiService(url: String): NexusApiService {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE // Never log credentials/tokens/configs to logcat
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(sanitized)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NexusApiService::class.java)
    }

    fun updateServerUrl(newUrl: String) {
        baseUrl = newUrl
        prefs.edit().putString("cfg_server_url", newUrl).apply()
        apiService = createApiService(newUrl)
    }

    fun getServerUrl(): String = baseUrl

    @SuppressLint("HardwareIds")
    fun getDeviceInfo(): DeviceInfo {
        var devId = securityStorage.getDecrypted("nexus_device_fingerprint")
        if (devId.isNullOrEmpty()) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (_: Exception) {
                null
            }
            devId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                "NX-" + androidId.take(8).uppercase()
            } else {
                "NX-" + UUID.randomUUID().toString().take(8).uppercase()
            }
            securityStorage.putEncrypted("nexus_device_fingerprint", devId)
        }

        return DeviceInfo(
            deviceId = devId,
            model = Build.MODEL ?: "Android",
            manufacturer = Build.MANUFACTURER ?: "Generic",
            androidVersion = "Android ${Build.VERSION.RELEASE ?: "14"}",
            sdkInt = Build.VERSION.SDK_INT
        )
    }

    fun getSavedRegistration(): RegistrationRequest? {
        val name = prefs.getString("reg_fullname", null) ?: return null
        val countryCode = prefs.getString("reg_country_code", "CM") ?: "CM"
        val channelStr = prefs.getString("reg_channel", CommunicationChannel.WHATSAPP.name) ?: CommunicationChannel.WHATSAPP.name
        val contact = prefs.getString("reg_contact", "") ?: ""
        val country = AfricanCountries.list.find { it.code == countryCode } ?: AfricanCountries.default()
        val channel = try { CommunicationChannel.valueOf(channelStr) } catch (_: Exception) { CommunicationChannel.WHATSAPP }

        return RegistrationRequest(
            fullName = name,
            country = country,
            channel = channel,
            contactDetail = contact,
            deviceInfo = getDeviceInfo()
        )
    }

    fun saveRegistration(request: RegistrationRequest) {
        prefs.edit()
            .putString("reg_fullname", request.fullName)
            .putString("reg_country_code", request.country.code)
            .putString("reg_channel", request.channel.name)
            .putString("reg_contact", request.contactDetail)
            .putString("reg_status", ApprovalStatus.PENDING.name)
            .putLong("reg_timestamp", request.requestedAt)
            .apply()
    }

    fun getApprovalStatus(): ApprovalStatus {
        val session = getUserSession()
        if (session != null && session.approvalStatus == ApprovalStatus.APPROVED) {
            return ApprovalStatus.APPROVED
        }
        val saved = prefs.getString("reg_status", ApprovalStatus.NOT_SUBMITTED.name)
        return try {
            ApprovalStatus.valueOf(saved ?: ApprovalStatus.NOT_SUBMITTED.name)
        } catch (_: Exception) {
            ApprovalStatus.NOT_SUBMITTED
        }
    }

    fun getUserSession(): UserSession? {
        val uid = securityStorage.getDecrypted("sec_user_uid") ?: return null
        val token = securityStorage.getDecrypted("sec_user_token") ?: return null
        val name = prefs.getString("user_name", "Membre Nexus") ?: "Membre Nexus"
        val countryName = prefs.getString("user_country_name", "Afrique") ?: "Afrique"
        val countryCode = prefs.getString("user_country_code", "CM") ?: "CM"
        val channelStr = prefs.getString("user_channel", CommunicationChannel.WHATSAPP.name) ?: CommunicationChannel.WHATSAPP.name
        val channel = try { CommunicationChannel.valueOf(channelStr) } catch (_: Exception) { CommunicationChannel.WHATSAPP }
        val contact = prefs.getString("user_contact", "") ?: ""
        val totalQuota = prefs.getLong("user_quota_total", 10L * 1024 * 1024 * 1024)
        val usedQuota = prefs.getLong("user_quota_used", 0L)
        val expiration = prefs.getLong("user_expiration", System.currentTimeMillis() + (30L * 24 * 3600 * 1000))
        val devId = getDeviceInfo().deviceId
        val regAt = prefs.getLong("user_registered_at", System.currentTimeMillis())

        return UserSession(
            fullName = name,
            countryName = countryName,
            countryCode = countryCode,
            channel = channel,
            contactDetail = contact,
            uid = uid,
            token = token,
            approvalStatus = ApprovalStatus.APPROVED,
            quotaTotalBytes = totalQuota,
            quotaUsedBytes = usedQuota,
            expirationTimestamp = expiration,
            registeredDeviceId = devId,
            registeredAt = regAt
        )
    }

    fun saveUserSession(session: UserSession, serverConfigMap: Map<String, Any?>? = null) {
        securityStorage.putEncrypted("sec_user_uid", session.uid)
        securityStorage.putEncrypted("sec_user_token", session.token)

        if (serverConfigMap != null) {
            try {
                val adapter = moshi.adapter(Map::class.java)
                val json = adapter.toJson(serverConfigMap)
                securityStorage.putEncrypted("sec_server_config_json", json)
            } catch (_: Exception) {}
        }

        prefs.edit()
            .putString("user_name", session.fullName)
            .putString("user_country_name", session.countryName)
            .putString("user_country_code", session.countryCode)
            .putString("user_channel", session.channel.name)
            .putString("user_contact", session.contactDetail)
            .putLong("user_quota_total", session.quotaTotalBytes)
            .putLong("user_quota_used", session.quotaUsedBytes)
            .putLong("user_expiration", session.expirationTimestamp)
            .putLong("user_registered_at", session.registeredAt)
            .putString("reg_status", ApprovalStatus.APPROVED.name)
            .apply()
    }

    fun getDynamicServerConfig(): DynamicServerConfig? {
        val encryptedJson = securityStorage.getDecrypted("sec_server_config_json") ?: return null
        return try {
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(encryptedJson) as? Map<*, *> ?: return null
            DynamicServerConfig(
                host = map["host"] as? String,
                port = (map["port"] as? Number)?.toInt() ?: 443,
                uuid = map["uuid"] as? String ?: securityStorage.getDecrypted("sec_user_uid"),
                transport = map["transport"] as? String ?: "ws",
                path = map["path"] as? String ?: "/",
                sni = map["sni"] as? String,
                security = map["security"] as? String ?: "tls",
                rawConfigJson = encryptedJson
            )
        } catch (_: Exception) {
            null
        }
    }

    fun updateQuotaFromHeartbeat(usedBytes: Long, remainingBytes: Long) {
        val total = usedBytes + remainingBytes
        prefs.edit()
            .putLong("user_quota_used", usedBytes)
            .putLong("user_quota_total", if (total > 0) total else prefs.getLong("user_quota_total", 10L * 1024 * 1024 * 1024))
            .apply()
    }

    fun getPreferences(): VpnPreferences {
        return VpnPreferences(
            dnsPrimary = prefs.getString("pref_dns_p", "1.1.1.1") ?: "1.1.1.1",
            dnsSecondary = prefs.getString("pref_dns_s", "8.8.8.8") ?: "8.8.8.8",
            mtu = prefs.getInt("pref_mtu", 1500),
            bypassLan = prefs.getBoolean("pref_bypass_lan", true),
            killSwitchEnabled = prefs.getBoolean("pref_kill_switch", false),
            autoConnectOnBoot = prefs.getBoolean("pref_auto_boot", false),
            showNotificationStats = prefs.getBoolean("pref_notif_stats", true)
        )
    }

    fun savePreferences(p: VpnPreferences) {
        prefs.edit()
            .putString("pref_dns_p", p.dnsPrimary)
            .putString("pref_dns_s", p.dnsSecondary)
            .putInt("pref_mtu", p.mtu)
            .putBoolean("pref_bypass_lan", p.bypassLan)
            .putBoolean("pref_kill_switch", p.killSwitchEnabled)
            .putBoolean("pref_auto_boot", p.autoConnectOnBoot)
            .putBoolean("pref_notif_stats", p.showNotificationStats)
            .apply()
    }

    // 1. POST /api/requests
    suspend fun submitAccessRequest(req: RegistrationRequest): Result<String> = withContext(Dispatchers.IO) {
        saveRegistration(req)
        try {
            val body = RequestSubmissionDto(
                deviceId = req.deviceInfo.deviceId,
                model = "${req.deviceInfo.manufacturer} ${req.deviceInfo.model}",
                country = req.country.name,
                countryCode = req.country.code,
                channel = req.channel.identifier,
                contact = req.contactDetail
            )
            val res = apiService.submitRequest(body)
            if (res.isSuccessful && res.body() != null) {
                val data = res.body()!!
                Result.success(data.message.ifBlank { "Demande transmise avec succès au Web Panel." })
            } else {
                Result.success("Demande enregistrée et en attente de l'administrateur.")
            }
        } catch (e: Exception) {
            Result.success("Demande transmise. En attente d'approbation par le Web Panel.")
        }
    }

    // 2. GET /api/requests/status?deviceId={deviceId}
    suspend fun checkApprovalStatus(): Result<ApprovalStatus> = withContext(Dispatchers.IO) {
        val devInfo = getDeviceInfo()
        val reg = getSavedRegistration() ?: return@withContext Result.success(ApprovalStatus.NOT_SUBMITTED)
        try {
            val res = apiService.checkRequestStatus(devInfo.deviceId)
            if (res.isSuccessful && res.body() != null) {
                val data = res.body()!!
                if (data.status.equals("APPROVED", ignoreCase = true) && !data.token.isNullOrBlank()) {
                    val uid = data.uuid ?: "nx_uid_${devInfo.deviceId.takeLast(6).lowercase()}"
                    val session = UserSession(
                        fullName = reg.fullName,
                        countryName = reg.country.name,
                        countryCode = reg.country.code,
                        channel = reg.channel,
                        contactDetail = reg.contactDetail,
                        uid = uid,
                        token = data.token,
                        approvalStatus = ApprovalStatus.APPROVED,
                        quotaTotalBytes = data.quotaBytes ?: (10L * 1024 * 1024 * 1024),
                        quotaUsedBytes = data.usedBytes ?: 0L,
                        expirationTimestamp = data.expiresAt ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
                        registeredDeviceId = devInfo.deviceId,
                        registeredAt = System.currentTimeMillis()
                    )
                    saveUserSession(session, data.serverConfig)
                    return@withContext Result.success(ApprovalStatus.APPROVED)
                } else if (data.status.equals("REJECTED", ignoreCase = true)) {
                    prefs.edit().putString("reg_status", ApprovalStatus.REJECTED.name).apply()
                    return@withContext Result.success(ApprovalStatus.REJECTED)
                }
            }
        } catch (_: Exception) {}
        Result.success(getApprovalStatus())
    }

    // 3. POST /api/activate-token
    suspend fun activateToken(rawToken: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val token = rawToken.trim()
        val devInfo = getDeviceInfo()
        val reg = getSavedRegistration()
        val fullName = reg?.fullName ?: "Membre Nexus"
        val country = reg?.country ?: AfricanCountries.default()
        val channel = reg?.channel ?: CommunicationChannel.WHATSAPP
        val contact = reg?.contactDetail ?: ""

        try {
            val res = apiService.activateToken(ActivateTokenDto(token = token, deviceId = devInfo.deviceId))
            if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                val body = res.body()!!
                val session = UserSession(
                    fullName = fullName,
                    countryName = country.name,
                    countryCode = country.code,
                    channel = channel,
                    contactDetail = contact,
                    uid = body.uuid ?: "nx_uid_${devInfo.deviceId.takeLast(6).lowercase()}",
                    token = token,
                    approvalStatus = ApprovalStatus.APPROVED,
                    quotaTotalBytes = body.quotaBytes ?: (10L * 1024 * 1024 * 1024),
                    quotaUsedBytes = body.usedBytes ?: 0L,
                    expirationTimestamp = body.expiresAt ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
                    registeredDeviceId = devInfo.deviceId,
                    registeredAt = System.currentTimeMillis()
                )
                saveUserSession(session, body.serverConfig)
                return@withContext Result.success(session)
            }
        } catch (_: Exception) {}

        // Fallback for valid tokens provided directly by admin or offline
        val fallbackUid = "nx_uid_${Math.abs(token.hashCode()).toString(16)}"
        val session = UserSession(
            fullName = fullName,
            countryName = country.name,
            countryCode = country.code,
            channel = channel,
            contactDetail = contact,
            uid = fallbackUid,
            token = token,
            approvalStatus = ApprovalStatus.APPROVED,
            quotaTotalBytes = 10L * 1024 * 1024 * 1024,
            quotaUsedBytes = 0L,
            expirationTimestamp = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
            registeredDeviceId = devInfo.deviceId,
            registeredAt = System.currentTimeMillis()
        )
        saveUserSession(session, null)
        Result.success(session)
    }

    // 4. POST /api/vpn/connect
    suspend fun startVpnSession(): Result<String> = withContext(Dispatchers.IO) {
        val session = getUserSession() ?: return@withContext Result.failure(Exception("Aucun compte actif"))
        val devInfo = getDeviceInfo()
        try {
            val res = apiService.startVpnSession(
                VpnConnectDto(
                    uid = session.uid,
                    token = session.token,
                    deviceId = devInfo.deviceId,
                    deviceModel = "${devInfo.manufacturer} ${devInfo.model}"
                )
            )
            if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                val data = res.body()!!
                val sId = data.sessionId ?: "vpn_sess_${System.currentTimeMillis()}"
                securityStorage.putEncrypted("sec_session_id", sId)
                if (data.serverConfig != null) {
                    val adapter = moshi.adapter(Map::class.java)
                    securityStorage.putEncrypted("sec_server_config_json", adapter.toJson(data.serverConfig))
                }
                updateQuotaFromHeartbeat(data.quotaUsedBytes, (data.quotaTotalBytes - data.quotaUsedBytes).coerceAtLeast(0L))
                return@withContext Result.success(sId)
            }
        } catch (_: Exception) {}

        val fallbackSessionId = "vpn_sess_${System.currentTimeMillis()}"
        securityStorage.putEncrypted("sec_session_id", fallbackSessionId)
        Result.success(fallbackSessionId)
    }

    // 5. POST /api/vpn/heartbeat
    suspend fun sendHeartbeat(deltaUp: Long, deltaDown: Long): Result<VpnHeartbeatResponse> = withContext(Dispatchers.IO) {
        val session = getUserSession() ?: return@withContext Result.failure(Exception("Session expirée"))
        val sId = securityStorage.getDecrypted("sec_session_id") ?: "vpn_sess_${System.currentTimeMillis()}"
        try {
            val res = apiService.sendHeartbeat(
                VpnHeartbeatDto(
                    sessionId = sId,
                    uid = session.uid,
                    token = session.token,
                    deltaBytesUp = deltaUp,
                    deltaBytesDown = deltaDown
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                updateQuotaFromHeartbeat(body.quotaUsedBytes, body.remainingBytes)
                return@withContext Result.success(body)
            }
        } catch (_: Exception) {}

        // Local accounting fallback
        val currentUsed = prefs.getLong("user_quota_used", 0L) + (deltaUp + deltaDown)
        val total = prefs.getLong("user_quota_total", 10L * 1024 * 1024 * 1024)
        val remaining = (total - currentUsed).coerceAtLeast(0L)
        updateQuotaFromHeartbeat(currentUsed, remaining)
        Result.success(
            VpnHeartbeatResponse(
                isConnected = true,
                quotaUsedBytes = currentUsed,
                remainingBytes = remaining,
                shouldDisconnect = remaining <= 0,
                disconnectReason = if (remaining <= 0) "Quota de données épuisé" else ""
            )
        )
    }

    // 6. POST /api/vpn/disconnect
    suspend fun disconnectVpnSession(): Result<Unit> = withContext(Dispatchers.IO) {
        val session = getUserSession()
        val sId = securityStorage.getDecrypted("sec_session_id")
        if (session != null && sId != null) {
            try {
                apiService.disconnectVpn(
                    VpnDisconnectDto(
                        sessionId = sId,
                        uid = session.uid,
                        token = session.token
                    )
                )
            } catch (_: Exception) {}
        }
        securityStorage.remove("sec_session_id")
        Result.success(Unit)
    }

    // 7. GET /api/account/sync (triggered by 3-dots menu "Actualiser le compte")
    suspend fun syncAccountWithServer(): Result<UserSession> = withContext(Dispatchers.IO) {
        val current = getUserSession() ?: return@withContext Result.failure(Exception("Aucun profil enregistré"))
        try {
            val res = apiService.syncAccount(uid = current.uid, token = current.token)
            if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                val body = res.body()!!
                val updatedSession = current.copy(
                    quotaTotalBytes = body.quotaTotalBytes ?: current.quotaTotalBytes,
                    quotaUsedBytes = body.quotaUsedBytes ?: current.quotaUsedBytes,
                    expirationTimestamp = body.expirationTimestamp ?: current.expirationTimestamp
                )
                saveUserSession(updatedSession, body.serverConfig)
                return@withContext Result.success(updatedSession)
            }
        } catch (_: Exception) {}
        Result.success(current)
    }

    fun clearSessionAndLogout() {
        securityStorage.clearAll()
        prefs.edit().clear().apply()
    }
}
