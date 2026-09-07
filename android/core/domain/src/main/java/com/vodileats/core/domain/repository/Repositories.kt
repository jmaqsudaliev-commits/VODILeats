package com.vodileats.core.domain.repository

import com.vodileats.core.common.result.Resource
import com.vodileats.core.domain.model.Category
import com.vodileats.core.domain.model.DeliveryAddress
import com.vodileats.core.domain.model.Order
import com.vodileats.core.domain.model.OrderStatus
import com.vodileats.core.domain.model.Restaurant
import com.vodileats.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun sendOtp(phone: String): Resource<String>
    suspend fun verifyOtp(phone: String, otp: String, firstName: String?, lastName: String?, role: String?): Resource<UserProfile>
    suspend fun logout(): Resource<Unit>
    fun isLoggedIn(): Flow<Boolean>
}

interface RestaurantRepository {
    suspend fun getRestaurants(
        search: String? = null,
        page: Int = 1,
        latitude: Double? = null,
        longitude: Double? = null
    ): Resource<List<Restaurant>>

    suspend fun getRestaurantDetail(id: String): Resource<Restaurant>

    suspend fun getDeliveryFee(
        restaurantId: String,
        latitude: Double,
        longitude: Double
    ): Resource<Pair<Double, Double>> // distanceKm, deliveryFee

    suspend fun getMenu(restaurantId: String): Resource<List<Category>>
}

interface OrderRepository {
    suspend fun createOrder(
        restaurantId: String,
        deliveryAddress: String,
        deliveryLatitude: Double,
        deliveryLongitude: Double,
        paymentMethod: String,
        customerNote: String?,
        items: List<Map<String, Any>>
    ): Resource<Order>

    suspend fun getMyOrders(page: Int = 1): Resource<List<Order>>
    suspend fun getOrderDetail(id: String): Resource<Order>
    suspend fun cancelOrder(id: String, reason: String): Resource<Order>

    // Restoran
    suspend fun getActiveOrders(restaurantId: String): Resource<List<Order>>
    suspend fun confirmOrder(id: String): Resource<Order>
    suspend fun startPreparing(id: String): Resource<Order>
    suspend fun markReady(id: String): Resource<Order>

    // Kuryer
    suspend fun markPickedUp(id: String): Resource<Order>
    suspend fun startDelivering(id: String): Resource<Order>
    suspend fun markDelivered(id: String): Resource<Order>
}

interface CourierRepository {
    suspend fun updateStatus(status: String): Resource<Unit>
    suspend fun updateLocation(latitude: Double, longitude: Double): Resource<Unit>
    suspend fun acceptOrder(orderId: String): Resource<Unit>
    suspend fun rejectOrder(orderId: String): Resource<Unit>
    suspend fun completeDelivery(): Resource<Unit>
}

interface UserRepository {
    suspend fun getProfile(): Resource<UserProfile>
    suspend fun updateProfile(firstName: String, lastName: String): Resource<UserProfile>
    suspend fun getAddresses(): Resource<List<DeliveryAddress>>
    suspend fun addAddress(data: Map<String, Any>): Resource<DeliveryAddress>
}
