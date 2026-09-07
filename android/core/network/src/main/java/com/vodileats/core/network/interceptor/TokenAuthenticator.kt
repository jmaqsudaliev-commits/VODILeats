package com.vodileats.core.network.interceptor

import com.vodileats.core.network.api.AuthApi
import com.vodileats.core.network.auth.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator - 401 javob kelganda refresh token orqali
 * yangi access token oladi va so'rovni qaytadan jo'natadi.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiProvider: dagger.Lazy<AuthApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Agar allaqachon 2 marta urinilgan bo'lsa, null qaytarish (cheksiz loop oldini olish)
        if (responseCount(response) >= 2) {
            return null
        }

        val refreshToken = tokenManager.getRefreshTokenSync() ?: return null

        return runBlocking {
            try {
                val result = authApiProvider.get().refreshToken(
                    mapOf("refreshToken" to refreshToken)
                )

                if (result.isSuccessful && result.body() != null) {
                    val body = result.body()!!
                    val newAccessToken = body["accessToken"] as? String ?: return@runBlocking null
                    val newRefreshToken = body["refreshToken"] as? String ?: refreshToken

                    tokenManager.saveTokens(newAccessToken, newRefreshToken)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                } else {
                    // Refresh token ham eskirgan - logout
                    tokenManager.clearAll()
                    null
                }
            } catch (e: Exception) {
                tokenManager.clearAll()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }
}
