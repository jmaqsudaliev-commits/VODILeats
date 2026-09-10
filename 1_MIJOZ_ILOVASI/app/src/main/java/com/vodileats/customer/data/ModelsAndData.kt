package com.vodileats.customer.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Domain / DTO Models
data class Restaurant(
    val id: String,
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val totalReviews: Int,
    val deliveryFeeBase: Double,
    val avgDeliveryTimeMinutes: Int,
    val imageUrl: String?,
    val isOpen: Boolean = true
)

data class RestaurantListResponse(
    val data: List<Restaurant>,
    val total: Int,
    val page: Int,
    val limit: Int
)

data class Category(
    val id: String,
    val name: String,
    val items: List<MenuItem>
)

data class MenuItem(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val imageUrl: String?,
    val isAvailable: Boolean = true
)

data class Order(
    val id: String,
    val orderNumber: String,
    val status: String,
    val totalAmount: Double,
    val deliveryFee: Double,
    val deliveryAddress: String,
    val courierName: String? = null,
    val courierPhone: String? = null,
    val courierLat: Double? = null,
    val courierLng: Double? = null
)

// Room Cart
@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey
    val menuItemId: String,
    val restaurantId: String,
    val restaurantName: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String?
)

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getAll(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: CartItem)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE menuItemId = :id")
    suspend fun updateQuantity(id: String, quantity: Int): Int

    @Delete
    suspend fun delete(item: CartItem): Int

    @Query("DELETE FROM cart_items WHERE menuItemId = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM cart_items")
    suspend fun clear(): Int

    @Query("SELECT COUNT(*) FROM cart_items")
    fun getCount(): Flow<Int>
}

@Database(entities = [CartItem::class], version = 1, exportSchema = false)
abstract class CustomerDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}

// Retrofit API
interface CustomerApi {
    @GET("restaurants")
    suspend fun getRestaurants(
        @retrofit2.http.Query("search") search: String? = null,
        @retrofit2.http.Query("page") page: Int = 1
    ): Response<RestaurantListResponse>

    @GET("restaurants/{id}")
    suspend fun getRestaurant(@Path("id") id: String): Response<Restaurant>

    @GET("menu/restaurant/{restaurantId}")
    suspend fun getMenu(@Path("restaurantId") restaurantId: String): Response<List<Category>>

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: Map<String, String>): Response<Map<String, String>>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("auth/login-password")
    suspend fun loginPassword(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("orders")
    suspend fun createOrder(@Body body: Map<String, Any>): Response<Order>

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: String): Response<Order>
}

// Preferences Manager for Customer
object CustomerPrefs {
    private const val PREF_NAME = "vodil_customer_prefs"

    fun saveUser(context: Context, token: String, phone: String, name: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("token", token)
            .putString("phone", phone)
            .putString("name", name)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean("is_logged_in", false)
    }

    fun getUserPhone(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("phone", "") ?: ""
    }

    fun getUserName(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("name", "Mijoz") ?: "Mijoz"
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

// Interceptor to dynamically route calls to whatever IP/Domain the user sets (Production Server, Real WiFi phone, or Emulator)
class DynamicHostInterceptor(private val context: Context) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        var request = chain.request()
        val rawInput = CustomerPrefs.getServerHost(context).trim()

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

// Hilt Dependency Injection
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val BASE_URL = "http://192.168.1.20:3000/api/v1/"

    @Provides
    @Singleton
    fun provideOkHttp(@ApplicationContext context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return OkHttpClient.Builder()
            .addInterceptor(DynamicHostInterceptor(context))
            .addInterceptor(logging)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideApi(okHttpClient: OkHttpClient): CustomerApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CustomerApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CustomerDatabase {
        return Room.databaseBuilder(context, CustomerDatabase::class.java, "customer_cart.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideCartDao(db: CustomerDatabase): CartDao = db.cartDao()
}
