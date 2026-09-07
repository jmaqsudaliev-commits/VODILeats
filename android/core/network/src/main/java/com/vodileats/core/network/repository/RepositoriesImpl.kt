package com.vodileats.core.network.repository

import com.vodileats.core.common.result.Resource
import com.vodileats.core.domain.model.*
import com.vodileats.core.domain.repository.*
import com.vodileats.core.network.api.*
import com.vodileats.core.network.auth.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun sendOtp(phone: String): Resource<String> {
        return try {
            val response = authApi.sendOtp(mapOf("phone" to phone))
            if (response.isSuccessful && response.body() != null) {
                val msg = response.body()!!["message"] ?: "OTP yuborildi"
                Resource.Success(msg)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "OTP yuborishda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun verifyOtp(
        phone: String,
        otp: String,
        firstName: String?,
        lastName: String?,
        role: String?
    ): Resource<UserProfile> {
        return try {
            val body = mutableMapOf("phone" to phone, "otp" to otp)
            firstName?.let { body["firstName"] = it }
            lastName?.let { body["lastName"] = it }
            role?.let { body["role"] = it }

            val response = authApi.verifyOtp(body)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val accessToken = data["accessToken"] as? String ?: ""
                val refreshToken = data["refreshToken"] as? String ?: ""
                val userMap = data["user"] as? Map<*, *>

                val profile = UserProfile(
                    id = userMap?.get("id") as? String ?: "",
                    phone = userMap?.get("phone") as? String ?: phone,
                    firstName = userMap?.get("firstName") as? String,
                    lastName = userMap?.get("lastName") as? String,
                    role = UserRole.valueOf(userMap?.get("role") as? String ?: "CUSTOMER")
                )

                tokenManager.saveTokens(accessToken, refreshToken, profile.role.name)
                Resource.Success(profile)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Tasdiqlash kodi noto'g'ri")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun logout(): Resource<Unit> {
        return try {
            tokenManager.clearTokens()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Xatolik")
        }
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return tokenManager.getAccessToken().map { !it.isNullOrBlank() }
    }
}

@Singleton
class RestaurantRepositoryImpl @Inject constructor(
    private val restaurantApi: RestaurantApi
) : RestaurantRepository {

    override suspend fun getRestaurants(
        search: String?,
        page: Int,
        latitude: Double?,
        longitude: Double?
    ): Resource<List<Restaurant>> {
        return try {
            val response = restaurantApi.getRestaurants(
                search = search,
                page = page,
                latitude = latitude,
                longitude = longitude
            )
            if (response.isSuccessful && response.body() != null) {
                val domainList = response.body()!!.data.map { dto ->
                    Restaurant(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description ?: "",
                        address = dto.address,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        rating = dto.rating,
                        ratingCount = dto.ratingCount,
                        deliveryFee = dto.deliveryFee,
                        estimatedDeliveryMinutes = dto.estimatedDeliveryMinutes,
                        imageUrl = dto.imageUrl ?: "",
                        isOpen = dto.isOpen
                    )
                }
                Resource.Success(domainList)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Restoranlarni yuklashda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun getRestaurantDetail(id: String): Resource<Restaurant> {
        return try {
            val response = restaurantApi.getRestaurant(id)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                Resource.Success(
                    Restaurant(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description ?: "",
                        address = dto.address,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        rating = dto.rating,
                        ratingCount = dto.ratingCount,
                        deliveryFee = dto.deliveryFee,
                        estimatedDeliveryMinutes = dto.estimatedDeliveryMinutes,
                        imageUrl = dto.imageUrl ?: "",
                        isOpen = dto.isOpen
                    )
                )
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Restoran topilmadi")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun getDeliveryFee(
        restaurantId: String,
        latitude: Double,
        longitude: Double
    ): Resource<Pair<Double, Double>> {
        return try {
            val response = restaurantApi.calculateDeliveryFee(restaurantId, latitude, longitude)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                Resource.Success(Pair(dto.distanceKm, dto.deliveryFee))
            } else {
                Resource.Error("Yetkazish narxini hisoblab bo'lmadi")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun getMenu(restaurantId: String): Resource<List<Category>> {
        return try {
            val response = restaurantApi.getMenu(restaurantId)
            if (response.isSuccessful && response.body() != null) {
                val categories = response.body()!!.map { catDto ->
                    Category(
                        id = catDto.id,
                        name = catDto.name,
                        items = catDto.items.map { itemDto ->
                            MenuItem(
                                id = itemDto.id,
                                name = itemDto.name,
                                description = itemDto.description ?: "",
                                price = itemDto.price,
                                imageUrl = itemDto.imageUrl ?: "",
                                isAvailable = itemDto.isAvailable,
                                restaurantId = restaurantId
                            )
                        }
                    )
                }
                Resource.Success(categories)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Menyuni yuklashda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }
}
