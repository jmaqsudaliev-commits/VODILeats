package com.vodileats.restaurant.data

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

// Preferences for Restaurant
object RestaurantPrefs {
    private const val PREF_NAME = "vodil_restaurant_prefs"

    fun saveRestaurant(context: Context, id: String, name: String, accessCode: String, isOpen: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("restaurant_id", id)
            .putString("restaurant_name", name)
            .putString("access_code", accessCode)
            .putBoolean("is_open", isOpen)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean("is_logged_in", false)
    }

    fun getRestaurantId(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("restaurant_id", "") ?: ""
    }

    fun getRestaurantName(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("restaurant_name", "Shohona Osh") ?: "Shohona Osh"
    }

    fun getAccessCode(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("access_code", "") ?: ""
    }

    fun isOpen(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean("is_open", true)
    }

    fun setOpen(context: Context, isOpen: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean("is_open", isOpen).apply()
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
        val rawInput = RestaurantPrefs.getServerHost(context).trim()

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
interface RestaurantApi {
    @POST("restaurants/login-by-code")
    suspend fun loginByCode(@Body body: Map<String, String>): Response<Map<String, Any>>

    @PUT("restaurants/{id}/toggle-open")
    suspend fun toggleOpen(@Path("id") id: String): Response<Map<String, Any>>

    @GET("admin/orders")
    suspend fun getOrders(@Query("status") status: String? = null): Response<List<Map<String, Any>>>

    @PUT("admin/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>
}

// Hilt Module
@Module
@InstallIn(SingletonComponent::class)
object RestaurantDataModule {
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
    fun provideRestaurantApi(okHttpClient: OkHttpClient): RestaurantApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RestaurantApi::class.java)
    }
}
