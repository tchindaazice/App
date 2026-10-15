package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

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
    val success: Boolean,
    val message: String,
    val status: String, // "PENDING", "APPROVED", "REJECTED"
    val uid: String? = null,
    val token: String? = null,
    val quotaBytes: Long? = null,
    val expirationTimestamp: Long? = null
)

data class ApiTokenVerifyRequest(
    val token: String,
    val deviceId: String
)

data class ApiQuotaResponse(
    val success: Boolean,
    val uid: String,
    val quotaTotalBytes: Long,
    val quotaUsedBytes: Long,
    val expirationTimestamp: Long,
    val isValid: Boolean
)

interface NexusApiService {
    @POST("api/membership/request")
    suspend fun submitMembership(
        @Body request: ApiMembershipRequest
    ): Response<ApiMembershipResponse>

    @GET("api/membership/status")
    suspend fun checkStatus(
        @Query("deviceId") deviceId: String,
        @Query("contact") contact: String
    ): Response<ApiMembershipResponse>

    @POST("api/token/verify")
    suspend fun verifyToken(
        @Body request: ApiTokenVerifyRequest
    ): Response<ApiMembershipResponse>

    @GET("api/user/quota")
    suspend fun getQuota(
        @Header("Authorization") bearerToken: String,
        @Query("uid") uid: String
    ): Response<ApiQuotaResponse>
}
