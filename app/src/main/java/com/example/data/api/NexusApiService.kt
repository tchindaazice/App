package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// 1. Soumission de demande d'accès : POST /api/requests
data class RequestSubmissionDto(
    val deviceId: String,
    val model: String,
    val country: String,
    val countryCode: String,
    val channel: String,
    val contact: String
)

data class RequestSubmissionResponse(
    val success: Boolean = true,
    val message: String = "",
    val status: String = "PENDING"
)

// 2. Vérification du statut d'approbation : GET /api/requests/status?deviceId={deviceId}
data class RequestStatusResponse(
    val success: Boolean = true,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val token: String? = null,
    val uuid: String? = null,
    val quotaBytes: Long? = null,
    val usedBytes: Long? = null,
    val expiresAt: Long? = null,
    val serverConfig: Map<String, Any?>? = null
)

// 3. Activation par Token : POST /api/activate-token
data class ActivateTokenDto(
    val token: String,
    val deviceId: String
)

data class ActivateTokenResponse(
    val success: Boolean = true,
    val message: String = "",
    val uuid: String? = null,
    val quotaBytes: Long? = null,
    val usedBytes: Long? = null,
    val expiresAt: Long? = null,
    val serverConfig: Map<String, Any?>? = null
)

// 4. Démarrage de session VPN : POST /api/vpn/connect
data class VpnConnectDto(
    val uid: String,
    val token: String,
    val deviceId: String,
    val deviceModel: String
)

data class VpnConnectResponse(
    val success: Boolean = true,
    val message: String = "",
    val sessionId: String? = null,
    val quotaTotalBytes: Long = 0,
    val quotaUsedBytes: Long = 0,
    val serverConfig: Map<String, Any?>? = null
)

// 5. Battement de Cœur et Quota Réel : POST /api/vpn/heartbeat
data class VpnHeartbeatDto(
    val sessionId: String,
    val uid: String,
    val token: String,
    val deltaBytesUp: Long,
    val deltaBytesDown: Long
)

data class VpnHeartbeatResponse(
    val isConnected: Boolean = true,
    val quotaUsedBytes: Long = 0,
    val remainingBytes: Long = 0,
    val shouldDisconnect: Boolean = false,
    val disconnectReason: String = ""
)

// 6. Déconnexion propre : POST /api/vpn/disconnect
data class VpnDisconnectDto(
    val sessionId: String,
    val uid: String,
    val token: String
)

data class VpnDisconnectResponse(
    val success: Boolean = true,
    val message: String = ""
)

// 7. Synchronisation de compte : GET /api/account/sync?uid={uid}&token={token}
data class AccountSyncResponse(
    val success: Boolean = true,
    val uid: String? = null,
    val quotaTotalBytes: Long? = null,
    val quotaUsedBytes: Long? = null,
    val expirationTimestamp: Long? = null,
    val serverConfig: Map<String, Any?>? = null
)

interface NexusApiService {

    @POST("api/requests")
    suspend fun submitRequest(
        @Body body: RequestSubmissionDto
    ): Response<RequestSubmissionResponse>

    @GET("api/requests/status")
    suspend fun checkRequestStatus(
        @Query("deviceId") deviceId: String
    ): Response<RequestStatusResponse>

    @POST("api/activate-token")
    suspend fun activateToken(
        @Body body: ActivateTokenDto
    ): Response<ActivateTokenResponse>

    @POST("api/vpn/connect")
    suspend fun startVpnSession(
        @Body body: VpnConnectDto
    ): Response<VpnConnectResponse>

    @POST("api/vpn/heartbeat")
    suspend fun sendHeartbeat(
        @Body body: VpnHeartbeatDto
    ): Response<VpnHeartbeatResponse>

    @POST("api/vpn/disconnect")
    suspend fun disconnectVpn(
        @Body body: VpnDisconnectDto
    ): Response<VpnDisconnectResponse>

    @GET("api/account/sync")
    suspend fun syncAccount(
        @Query("uid") uid: String,
        @Query("token") token: String
    ): Response<AccountSyncResponse>
}
