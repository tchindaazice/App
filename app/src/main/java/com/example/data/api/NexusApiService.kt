package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// 1. Demande d'adhésion : POST /api/membership/request (Web Panel Admin)
data class ApiMembershipRequest(
    val fullName: String,
    val country: String,
    val countryCode: String,
    val channel: String,
    val contactDetail: String,
    val deviceId: String,
    val deviceModel: String,
    val androidVersion: String,
    val timestamp: Long
)

data class ApiMembershipResponse(
    val success: Boolean = true,
    val message: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val uid: String? = null,
    val token: String? = null,
    val quotaBytes: Long? = null,
    val expirationTimestamp: Long? = null,
    val vlessConfig: Map<String, Any?>? = null
)

// 2. Vérification de statut : GET /api/membership/status?deviceId={id}&contact={c}
// Réponse utilise ApiMembershipResponse

// 3. Activation par Token : POST /api/token/verify
data class ApiTokenVerifyRequest(
    val token: String,
    val deviceId: String
)

// 4. Consultation de quota en temps réel : GET /api/user/quota?uid={uid}
data class ApiQuotaResponse(
    val success: Boolean = true,
    val message: String? = null,
    val uid: String = "",
    val quotaTotalBytes: Long = 0,
    val quotaUsedBytes: Long = 0,
    val expirationTimestamp: Long = 0,
    val isValid: Boolean = true
)

// 5. Rapport de trafic réel : POST /api/user/traffic
data class ApiUserTrafficRequest(
    val uid: String,
    val token: String,
    val bytesUp: Long,
    val bytesDown: Long
)

data class ApiUserTrafficResponse(
    val success: Boolean = true,
    val shouldDisconnect: Boolean = false,
    val reason: String? = null,
    val quotaUsedBytes: Long? = null,
    val remainingBytes: Long? = null
)

interface NexusApiService {

    // Endpoints officiels du Web Panel Nexus Tunnel
    @POST("api/membership/request")
    suspend fun submitMembership(
        @Body request: ApiMembershipRequest
    ): Response<ApiMembershipResponse>

    @GET("api/membership/status")
    suspend fun checkMembershipStatus(
        @Query("deviceId") deviceId: String,
        @Query("contact") contact: String
    ): Response<ApiMembershipResponse>

    @POST("api/token/verify")
    suspend fun verifyToken(
        @Body request: ApiTokenVerifyRequest
    ): Response<ApiMembershipResponse>

    @GET("api/user/quota")
    suspend fun getUserQuota(
        @Query("uid") uid: String
    ): Response<ApiQuotaResponse>

    @POST("api/user/traffic")
    suspend fun reportUserTraffic(
        @Body request: ApiUserTrafficRequest
    ): Response<ApiUserTrafficResponse>
}
