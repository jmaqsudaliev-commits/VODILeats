package com.vodileats.core.network.interceptor

import com.vodileats.core.network.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor - har bir so'rovga Authorization header qo'shadi
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Auth so'rovlariga token qo'shmaymiz
        if (originalRequest.url.encodedPath.contains("auth/")) {
            return chain.proceed(originalRequest)
        }

        val token = tokenManager.getAccessTokenSync()

        return if (token != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
