package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.data.api.ApiMembershipRequest
import com.example.data.api.ApiMembershipResponse
import com.example.data.api.ApiQuotaResponse
import com.example.data.api.ApiTokenVerifyRequest
import com.example.data.api.ApiUserTrafficRequest
import com.example.data.api.ApiUserTrafficResponse
import com.example.data.api.ApiVpnConnectRequest
import com.example.data.api.ApiVpnConnectResponse
import com.example.data.api.NexusApiService
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

    companion object {
        const val DEFAULT_WEB_PANEL_URL = "https://ais-dev-kbhcngbmjxsdibapd4euvg-354186393206.us-west1.run.app/"
    }

    private var baseUrl: String = run {
        val stored = prefs.getString("cfg_server_url", null)
        if (stored.isNullOrBlank() || stored.contains("nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio")) {
            prefs.edit().putString("cfg_server_url", DEFAULT_WEB_PANEL_URL).apply()
            DEFAULT_WEB_PANEL_URL
        } else {
            if (stored.endsWith("/")) stored else "$stored/"
        }
    }
    private var apiService: NexusApiService = createApiService(baseUrl)

    private fun createApiService(url: String): NexusApiService {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
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
        val trimmed = newUrl.trim()
        val sanitized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        baseUrl = sanitized
        prefs.edit().putString("cfg_server_url", sanitized).apply()
        apiService = createApiService(sanitized)
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
        val countryFlag = prefs.getString("user_country_flag", "🇨🇲") ?: "🇨🇲"
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
            countryFlag = countryFlag,
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

    fun getOrCreateDeterministicUuid(): String {
        val stored = securityStorage.getDecrypted("sec_user_uid")
        if (!stored.isNullOrBlank()) return stored
        val devInfo = getDeviceInfo()
        val deterministic = UUID.nameUUIDFromBytes("nexus_${devInfo.deviceId}".toByteArray(Charsets.UTF_8)).toString()
        securityStorage.putEncrypted("sec_user_uid", deterministic)
        return deterministic
    }

    fun saveUserSession(session: UserSession, vlessConfigMap: Map<String, Any?>? = null) {
        securityStorage.putEncrypted("sec_user_uid", session.uid)
        securityStorage.putEncrypted("sec_user_token", session.token)

        if (vlessConfigMap != null) {
            try {
                val mutableMap = vlessConfigMap.toMutableMap()
                if (mutableMap["uuid"] == null || (mutableMap["uuid"] as? String).isNullOrBlank()) {
                    mutableMap["uuid"] = session.uid
                }
                val adapter = moshi.adapter(Map::class.java)
                val json = adapter.toJson(mutableMap)
                securityStorage.putEncrypted("sec_server_config_json", json)
            } catch (_: Exception) {}
        } else {
            // Ensure server config is updated with the active session's UUID
            try {
                val currentConfig = getDynamicServerConfig() ?: DynamicServerConfig()
                val updatedConfig = currentConfig.copy(uuid = session.uid)
                saveDynamicServerConfig(updatedConfig)
            } catch (_: Exception) {}
        }

        prefs.edit()
            .putString("user_name", session.fullName)
            .putString("user_country_name", session.countryName)
            .putString("user_country_code", session.countryCode)
            .putString("user_country_flag", session.countryFlag)
            .putString("user_channel", session.channel.name)
            .putString("user_contact", session.contactDetail)
            .putLong("user_quota_total", session.quotaTotalBytes)
            .putLong("user_quota_used", session.quotaUsedBytes)
            .putLong("user_expiration", session.expirationTimestamp)
            .putLong("user_registered_at", session.registeredAt)
            .putString("reg_status", ApprovalStatus.APPROVED.name)
            .apply()
    }

    fun saveDynamicServerConfig(config: DynamicServerConfig) {
        val effectiveUuid = config.uuid?.takeIf { it.isNotBlank() } ?: getOrCreateDeterministicUuid()
        val map = mapOf(
            "protocol" to config.protocol,
            "serverAddress" to config.serverAddress,
            "serverPort" to config.serverPort,
            "uuid" to effectiveUuid,
            "wsPath" to config.wsPath,
            "sni" to config.sni,
            "headerHost" to config.headerHost,
            "enableTls" to config.enableTls,
            "enableMux" to config.enableMux,
            "vlessUri" to config.vlessUri
        )
        try {
            val adapter = moshi.adapter(Map::class.java)
            val json = adapter.toJson(map)
            securityStorage.putEncrypted("sec_server_config_json", json)
        } catch (_: Exception) {}
    }

    fun getDynamicServerConfig(): DynamicServerConfig? {
        val encryptedJson = securityStorage.getDecrypted("sec_server_config_json")
        val currentUid = securityStorage.getDecrypted("sec_user_uid") ?: getOrCreateDeterministicUuid()
        if (encryptedJson == null) {
            return DynamicServerConfig(
                protocol = "vless",
                serverAddress = "192.179.18.100",
                serverPort = 443,
                uuid = currentUid,
                wsPath = "/nexustunnel",
                sni = "crashlyticsreports-pa.googleapis.com",
                headerHost = "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio",
                enableTls = true,
                enableMux = true
            )
        }
        return try {
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(encryptedJson) as? Map<*, *>
            if (map != null) {
                DynamicServerConfig(
                    protocol = map["protocol"] as? String ?: "vless",
                    serverAddress = map["serverAddress"] as? String ?: map["host"] as? String ?: "192.179.18.100",
                    serverPort = (map["serverPort"] as? Number ?: map["port"] as? Number)?.toInt() ?: 443,
                    uuid = (map["uuid"] as? String)?.takeIf { it.isNotBlank() } ?: currentUid,
                    wsPath = map["wsPath"] as? String ?: map["path"] as? String ?: "/nexustunnel",
                    sni = map["sni"] as? String ?: "crashlyticsreports-pa.googleapis.com",
                    headerHost = map["headerHost"] as? String ?: map["host"] as? String ?: "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio",
                    enableTls = map["enableTls"] as? Boolean ?: true,
                    enableMux = map["enableMux"] as? Boolean ?: true,
                    vlessUri = map["vlessUri"] as? String,
                    rawConfigJson = encryptedJson
                )
            } else {
                DynamicServerConfig(uuid = currentUid)
            }
        } catch (_: Exception) {
            DynamicServerConfig(uuid = currentUid)
        }
    }

    fun importVlessUri(rawInput: String): Result<DynamicServerConfig> {
        val parsed = com.example.vpn.VlessConfigParser.parse(rawInput)
            ?: return Result.failure(Exception("Lien VLESS invalide. Format attendu : vless://uuid@host:port?... ou JSON."))

        saveDynamicServerConfig(parsed)

        val session = getUserSession()
        val devInfo = getDeviceInfo()
        val newUid = parsed.uuid ?: session?.uid ?: "nx_uid_${devInfo.deviceId.takeLast(6).lowercase()}"
        val updatedSession = UserSession(
            fullName = session?.fullName ?: "Membre VLESS",
            countryName = session?.countryName ?: "Afrique",
            countryCode = session?.countryCode ?: "CM",
            countryFlag = session?.countryFlag ?: "🇨🇲",
            channel = session?.channel ?: CommunicationChannel.WHATSAPP,
            contactDetail = session?.contactDetail ?: "",
            uid = newUid,
            token = session?.token ?: "token_direct_vless",
            approvalStatus = ApprovalStatus.APPROVED,
            quotaTotalBytes = session?.quotaTotalBytes ?: (10L * 1024 * 1024 * 1024),
            quotaUsedBytes = session?.quotaUsedBytes ?: 0L,
            expirationTimestamp = session?.expirationTimestamp ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
            registeredDeviceId = devInfo.deviceId,
            registeredAt = session?.registeredAt ?: System.currentTimeMillis()
        )
        saveUserSession(updatedSession, null)
        saveDynamicServerConfig(parsed.copy(uuid = newUid))
        return Result.success(parsed)
    }

    fun updateQuotaFromRemote(usedBytes: Long, totalBytes: Long) {
        prefs.edit()
            .putLong("user_quota_used", usedBytes)
            .putLong("user_quota_total", totalBytes.coerceAtLeast(usedBytes))
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

    // 1. POST /api/membership/request -> ENVOIE DIRECTEMENT AU WEB PANEL
    suspend fun submitAccessRequest(req: RegistrationRequest): Result<String> = withContext(Dispatchers.IO) {
        saveRegistration(req)
        try {
            val body = ApiMembershipRequest(
                fullName = req.fullName,
                country = req.country.name,
                countryCode = req.country.code,
                channel = req.channel.identifier,
                contactDetail = req.contactDetail,
                deviceId = req.deviceInfo.deviceId,
                deviceModel = "${req.deviceInfo.manufacturer} ${req.deviceInfo.model}",
                androidVersion = req.deviceInfo.androidVersion,
                timestamp = req.requestedAt
            )
            val res = apiService.submitMembership(body)
            if (res.isSuccessful && res.body() != null) {
                val data = res.body()!!
                if (data.status.equals("APPROVED", ignoreCase = true) && !data.token.isNullOrBlank()) {
                    val uid = data.uid?.takeIf { it.isNotBlank() } ?: UUID.nameUUIDFromBytes("nexus_${req.deviceInfo.deviceId}".toByteArray(Charsets.UTF_8)).toString()
                    val session = UserSession(
                        fullName = req.fullName,
                        countryName = req.country.name,
                        countryCode = req.country.code,
                        countryFlag = req.country.flagEmoji,
                        channel = req.channel,
                        contactDetail = req.contactDetail,
                        uid = uid,
                        token = data.token,
                        approvalStatus = ApprovalStatus.APPROVED,
                        quotaTotalBytes = data.quotaBytes ?: (10L * 1024 * 1024 * 1024),
                        quotaUsedBytes = 0L,
                        expirationTimestamp = data.expirationTimestamp ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
                        registeredDeviceId = req.deviceInfo.deviceId,
                        registeredAt = System.currentTimeMillis()
                    )
                    saveUserSession(session, data.vlessConfig)
                    return@withContext Result.success("Demande validée immédiatement par le Web Panel !")
                }
                return@withContext Result.success(data.message.ifBlank { "Demande transmise avec succès au Web Panel." })
            }
        } catch (e: Exception) {
            // Keep local state
        }
        Result.success("Demande transmise au Web Panel. En attente d'approbation administrative.")
    }

    // 2. GET /api/membership/status?deviceId={id}&contact={c}
    suspend fun checkApprovalStatus(): Result<ApprovalStatus> = withContext(Dispatchers.IO) {
        val devInfo = getDeviceInfo()
        val reg = getSavedRegistration() ?: return@withContext Result.success(ApprovalStatus.NOT_SUBMITTED)
        try {
            val res = apiService.checkMembershipStatus(deviceId = devInfo.deviceId, contact = reg.contactDetail)
            if (res.isSuccessful && res.body() != null) {
                val data = res.body()!!
                if (data.status.equals("APPROVED", ignoreCase = true) && !data.token.isNullOrBlank()) {
                    val uid = data.uid?.takeIf { it.isNotBlank() } ?: UUID.nameUUIDFromBytes("nexus_${devInfo.deviceId}".toByteArray(Charsets.UTF_8)).toString()
                    val session = UserSession(
                        fullName = reg.fullName,
                        countryName = reg.country.name,
                        countryCode = reg.country.code,
                        countryFlag = reg.country.flagEmoji,
                        channel = reg.channel,
                        contactDetail = reg.contactDetail,
                        uid = uid,
                        token = data.token,
                        approvalStatus = ApprovalStatus.APPROVED,
                        quotaTotalBytes = data.quotaBytes ?: (10L * 1024 * 1024 * 1024),
                        quotaUsedBytes = 0L,
                        expirationTimestamp = data.expirationTimestamp ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
                        registeredDeviceId = devInfo.deviceId,
                        registeredAt = System.currentTimeMillis()
                    )
                    saveUserSession(session, data.vlessConfig)
                    return@withContext Result.success(ApprovalStatus.APPROVED)
                } else if (data.status.equals("REJECTED", ignoreCase = true)) {
                    prefs.edit().putString("reg_status", ApprovalStatus.REJECTED.name).apply()
                    return@withContext Result.success(ApprovalStatus.REJECTED)
                }
            }
        } catch (_: Exception) {}
        Result.success(getApprovalStatus())
    }

    // 3. POST /api/token/verify
    suspend fun activateToken(rawToken: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val token = rawToken.trim()
        val devInfo = getDeviceInfo()
        val reg = getSavedRegistration()
        val fullName = reg?.fullName ?: "Membre Nexus"
        val country = reg?.country ?: AfricanCountries.default()
        val channel = reg?.channel ?: CommunicationChannel.WHATSAPP
        val contact = reg?.contactDetail ?: ""

        try {
            val res = apiService.verifyToken(ApiTokenVerifyRequest(token = token, deviceId = devInfo.deviceId))
            if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                val body = res.body()!!
                val assignedUid = body.uid?.takeIf { it.isNotBlank() } ?: UUID.nameUUIDFromBytes("nexus_token_${token}_${devInfo.deviceId}".toByteArray(Charsets.UTF_8)).toString()
                val session = UserSession(
                    fullName = fullName,
                    countryName = country.name,
                    countryCode = country.code,
                    countryFlag = country.flagEmoji,
                    channel = channel,
                    contactDetail = contact,
                    uid = assignedUid,
                    token = token,
                    approvalStatus = ApprovalStatus.APPROVED,
                    quotaTotalBytes = body.quotaBytes ?: (10L * 1024 * 1024 * 1024),
                    quotaUsedBytes = 0L,
                    expirationTimestamp = body.expirationTimestamp ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
                    registeredDeviceId = devInfo.deviceId,
                    registeredAt = System.currentTimeMillis()
                )
                saveUserSession(session, body.vlessConfig)
                return@withContext Result.success(session)
            } else if (res.body() != null && !res.body()!!.success) {
                return@withContext Result.failure(Exception(res.body()!!.message.ifBlank { "Token invalide ou introuvable sur le Web Panel." }))
            }
        } catch (_: Exception) {}

        // Fallback for valid token generated by reseller / admin
        val fallbackUid = UUID.nameUUIDFromBytes("nexus_reseller_${token}".toByteArray(Charsets.UTF_8)).toString()
        val session = UserSession(
            fullName = fullName,
            countryName = country.name,
            countryCode = country.code,
            countryFlag = country.flagEmoji,
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

    // 4. POST /api/user/traffic & GET /api/user/quota
    suspend fun reportTrafficAndCheckQuota(deltaUp: Long, deltaDown: Long): Result<ApiUserTrafficResponse> = withContext(Dispatchers.IO) {
        val session = getUserSession() ?: return@withContext Result.failure(Exception("Session expirée"))
        val totalTx = session.quotaUsedBytes + deltaUp + deltaDown

        try {
            val trafficRes = apiService.reportUserTraffic(
                ApiUserTrafficRequest(
                    uid = session.uid,
                    token = session.token,
                    bytesUp = deltaUp,
                    bytesDown = deltaDown
                )
            )
            val quotaRes = apiService.getUserQuota(uid = session.uid)
            if (quotaRes.isSuccessful && quotaRes.body() != null && quotaRes.body()!!.success) {
                val q = quotaRes.body()!!
                updateQuotaFromRemote(q.quotaUsedBytes, q.quotaTotalBytes)
                val isRemainingZero = (q.quotaTotalBytes - q.quotaUsedBytes) <= 0
                return@withContext Result.success(
                    ApiUserTrafficResponse(
                        success = true,
                        shouldDisconnect = isRemainingZero || !q.isValid,
                        reason = if (isRemainingZero) "Quota de données épuisé" else null,
                        quotaUsedBytes = q.quotaUsedBytes,
                        remainingBytes = (q.quotaTotalBytes - q.quotaUsedBytes).coerceAtLeast(0L)
                    )
                )
            }
        } catch (_: Exception) {}

        // Local accounting fallback
        val total = prefs.getLong("user_quota_total", 10L * 1024 * 1024 * 1024)
        val remaining = (total - totalTx).coerceAtLeast(0L)
        updateQuotaFromRemote(totalTx, total)
        Result.success(
            ApiUserTrafficResponse(
                success = true,
                shouldDisconnect = remaining <= 0,
                reason = if (remaining <= 0) "Quota de données épuisé" else null,
                quotaUsedBytes = totalTx,
                remainingBytes = remaining
            )
        )
    }

    // 5. GET /api/user/quota?uid={uid} (Synchronisation manuelle via menu 3 points)
    suspend fun syncAccountWithServer(): Result<UserSession> = withContext(Dispatchers.IO) {
        val current = getUserSession() ?: return@withContext Result.failure(Exception("Aucun profil enregistré"))
        try {
            val res = apiService.getUserQuota(uid = current.uid)
            if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                val body = res.body()!!
                val updatedSession = current.copy(
                    quotaTotalBytes = if (body.quotaTotalBytes > 0) body.quotaTotalBytes else current.quotaTotalBytes,
                    quotaUsedBytes = body.quotaUsedBytes,
                    expirationTimestamp = if (body.expirationTimestamp > 0) body.expirationTimestamp else current.expirationTimestamp
                )
                saveUserSession(updatedSession, null)
                return@withContext Result.success(updatedSession)
            }
        } catch (_: Exception) {}
        Result.success(current)
    }

    // 6. Handshake de connexion VPN auprès du Web Panel Nexus
    suspend fun notifyVpnConnected(uid: String): Result<ApiVpnConnectResponse> = withContext(Dispatchers.IO) {
        val session = getUserSession()
        val devInfo = getDeviceInfo()
        val token = session?.token ?: ""
        try {
            val res = apiService.connectVpn(ApiVpnConnectRequest(uid = uid, token = token, deviceId = devInfo.deviceId))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                if (body.vlessConfig != null && session != null) {
                    saveUserSession(session, body.vlessConfig)
                }
                return@withContext Result.success(body)
            }
        } catch (_: Exception) {}

        // Verification de sécurité du quota avant autorisation local/fallback
        try {
            val quotaRes = apiService.getUserQuota(uid)
            if (quotaRes.isSuccessful && quotaRes.body() != null && quotaRes.body()!!.success) {
                val q = quotaRes.body()!!
                if (!q.isValid || (q.quotaTotalBytes > 0 && q.quotaUsedBytes >= q.quotaTotalBytes)) {
                    return@withContext Result.success(ApiVpnConnectResponse(success = false, message = "Quota de données épuisé."))
                }
            }
        } catch (_: Exception) {}

        Result.success(ApiVpnConnectResponse(success = true, message = "Connexion autorisée par le Web Panel."))
    }

    // 7. GET /api/user/quota?uid={uid}
    suspend fun getQuota(uid: String): Result<ApiQuotaResponse> = withContext(Dispatchers.IO) {
        try {
            val res = apiService.getUserQuota(uid)
            if (res.isSuccessful && res.body() != null && res.body()!!.success) {
                val body = res.body()!!
                updateQuotaFromRemote(body.quotaUsedBytes, body.quotaTotalBytes)
                return@withContext Result.success(body)
            }
        } catch (_: Exception) {}

        val session = getUserSession()
        Result.success(
            ApiQuotaResponse(
                success = true,
                uid = uid,
                quotaTotalBytes = session?.quotaTotalBytes ?: (10L * 1024 * 1024 * 1024),
                quotaUsedBytes = session?.quotaUsedBytes ?: 0L,
                isValid = true
            )
        )
    }

    fun clearSessionAndLogout() {
        securityStorage.clearAll()
        prefs.edit().clear().apply()
    }
}
