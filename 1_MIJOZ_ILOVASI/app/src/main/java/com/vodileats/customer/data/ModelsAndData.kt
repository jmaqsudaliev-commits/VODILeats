package com.vodileats.customer.data

import android.content.Context
import androidx.room.*
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
    suspend fun updateQuantity(id: String, quantity: Int)

    @Delete
    suspend fun delete(item: CartItem)

    @Query("DELETE FROM cart_items WHERE menuItemId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

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
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1
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

// Hilt Dependency Injection
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // 192.168.1.4: Userning kompyuter IP manzili (Real telefon va Wi-Fi uchun)
    // 10.0.2.2: Standart Android Studio emulyatori uchun
    private const val BASE_URL = "http://192.168.1.4:3000/api/v1/"

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
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
