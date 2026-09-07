package com.vodileats.core.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/send-otp")
    suspend fun sendOtp(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("auth/logout")
    suspend fun logout(): Response<Map<String, String>>
}
