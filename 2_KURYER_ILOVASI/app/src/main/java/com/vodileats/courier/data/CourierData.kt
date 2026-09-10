package com.vodileats.courier.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Preferences for Courier
object CourierPrefs {
    private const val PREF_NAME = "vodil_courier_prefs"

    fun saveCourier(context: Context, token: String, phone: String, name: String, isVerified: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("token", token)
            .putString("phone", phone)
            .putString("name", name)
            .putBoolean("is_verified", isVerified)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean("is_logged_in", false)
    }

    fun isVerified(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean("is_verified", false)
    }

    fun setVerified(context: Context, isVerified: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean("is_verified", isVerified).apply()
    }

    fun getCourierPhone(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("phone", "") ?: ""
    }

    fun getCourierName(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("name", "Kuryer") ?: "Kuryer"
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun setServerHost(context: Context, host: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString("server_host", host.trim()).apply()
    }

    fun getServerHost(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("server_host", "192.168.1.20:3000") ?: "192.168.1.20:3000"
    }
}

// Dynamic Host Interceptor (Production Server, Real WiFi phone, or Emulator)
class DynamicHostInterceptor(private val context: Context) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        var request = chain.request()
        val rawInput = CourierPrefs.getServerHost(context).trim()

        if (rawInput.isNotBlank()) {
            try {
                val isHttps = rawInput.startsWith("https://", ignoreCase = true)
                val clean = rawInput
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .trimEnd('/')

                val host: String
                val port: Int

                if (clean.contains(":")) {
                    val parts = clean.split(":")
                    host = parts[0]
                    port = parts[1].toIntOrNull() ?: if (isHttps) 443 else 80
                } else {
                    host = clean
                    port = if (host == "10.0.2.2" || host.startsWith("192.168.") || host == "localhost") 3000
                           else if (isHttps) 443 else 80
                }

                if (host.isNotBlank()) {
                    val newUrl = request.url.newBuilder()
                        .scheme(if (isHttps || port == 443) "https" else "http")
                        .host(host)
                        .port(port)
                        .build()
                    request = request.newBuilder().url(newUrl).build()
                }
            } catch (e: Exception) {}
        }

        return chain.proceed(request)
    }
}

// Retrofit API
interface CourierApi {
    @POST("courier/register")
    suspend fun register(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("courier/login")
    suspend fun login(@Body body: Map<String, String>): Response<Map<String, Any>>

    @GET("courier/status-check")
    suspend fun checkStatus(@Query("phone") phone: String): Response<Map<String, Any>>

    @PUT("courier/status")
    suspend fun updateStatus(@Body body: Map<String, String>): Response<Map<String, Any>>

    @PUT("courier/location")
    suspend fun updateLocation(@Body body: Map<String, Double>): Response<Map<String, Any>>
}

// Hilt Module
@Module
@InstallIn(SingletonComponent::class)
object CourierDataModule {
    private const val BASE_URL = "http://192.168.1.20:3000/api/v1/"

    @Provides
    @Singleton
    fun provideOkHttp(@ApplicationContext context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(DynamicHostInterceptor(context))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideCourierApi(okHttpClient: OkHttpClient): CourierApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CourierApi::class.java)
    }
}
