package com.vodileats.core.domain.usecase

import com.vodileats.core.common.result.Resource
import com.vodileats.core.domain.model.Category
import com.vodileats.core.domain.model.Order
import com.vodileats.core.domain.model.Restaurant
import com.vodileats.core.domain.model.UserProfile
import com.vodileats.core.domain.repository.AuthRepository
import com.vodileats.core.domain.repository.CourierRepository
import com.vodileats.core.domain.repository.OrderRepository
import com.vodileats.core.domain.repository.RestaurantRepository
import javax.inject.Inject

// ==================== Auth Use Cases ====================

class SendOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String): Resource<String> {
        return authRepository.sendOtp(phone)
    }
}

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        phone: String,
        otp: String,
        firstName: String? = null,
        lastName: String? = null,
        role: String? = null
    ): Resource<UserProfile> {
        return authRepository.verifyOtp(phone, otp, firstName, lastName, role)
    }
}

// ==================== Restaurant Use Cases ====================

class GetRestaurantsUseCase @Inject constructor(
    private val restaurantRepository: RestaurantRepository
) {
    suspend operator fun invoke(
        search: String? = null,
        page: Int = 1,
        latitude: Double? = null,
        longitude: Double? = null
    ): Resource<List<Restaurant>> {
        return restaurantRepository.getRestaurants(search, page, latitude, longitude)
    }
}

class GetRestaurantDetailUseCase @Inject constructor(
    private val restaurantRepository: RestaurantRepository
) {
    suspend operator fun invoke(id: String): Resource<Restaurant> {
        return restaurantRepository.getRestaurantDetail(id)
    }
}

class GetMenuUseCase @Inject constructor(
    private val restaurantRepository: RestaurantRepository
) {
    suspend operator fun invoke(restaurantId: String): Resource<List<Category>> {
        return restaurantRepository.getMenu(restaurantId)
    }
}

class GetDeliveryFeeUseCase @Inject constructor(
    private val restaurantRepository: RestaurantRepository
) {
    suspend operator fun invoke(
        restaurantId: String,
        latitude: Double,
        longitude: Double
    ): Resource<Pair<Double, Double>> {
        return restaurantRepository.getDeliveryFee(restaurantId, latitude, longitude)
    }
}

// ==================== Order Use Cases ====================

class CreateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(
        restaurantId: String,
        deliveryAddress: String,
        deliveryLatitude: Double,
        deliveryLongitude: Double,
        paymentMethod: String,
        customerNote: String?,
        items: List<Map<String, Any>>
    ): Resource<Order> {
        return orderRepository.createOrder(
            restaurantId,
            deliveryAddress,
            deliveryLatitude,
            deliveryLongitude,
            paymentMethod,
            customerNote,
            items
        )
    }
}

class GetMyOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(page: Int = 1): Resource<List<Order>> {
        return orderRepository.getMyOrders(page)
    }
}

class GetOrderDetailUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(id: String): Resource<Order> {
        return orderRepository.getOrderDetail(id)
    }
}

// ==================== Restoran Use Cases ====================

class GetActiveOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(restaurantId: String): Resource<List<Order>> {
        return orderRepository.getActiveOrders(restaurantId)
    }
}

class ConfirmOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String): Resource<Order> {
        return orderRepository.confirmOrder(orderId)
    }
}

class MarkOrderReadyUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String): Resource<Order> {
        return orderRepository.markReady(orderId)
    }
}

// ==================== Kuryer Use Cases ====================

class AcceptOrderUseCase @Inject constructor(
    private val courierRepository: CourierRepository
) {
    suspend operator fun invoke(orderId: String): Resource<Unit> {
        return courierRepository.acceptOrder(orderId)
    }
}

class RejectOrderUseCase @Inject constructor(
    private val courierRepository: CourierRepository
) {
    suspend operator fun invoke(orderId: String): Resource<Unit> {
        return courierRepository.rejectOrder(orderId)
    }
}

class UpdateCourierLocationUseCase @Inject constructor(
    private val courierRepository: CourierRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Resource<Unit> {
        return courierRepository.updateLocation(latitude, longitude)
    }
}

class CompletDeliveryUseCase @Inject constructor(
    private val courierRepository: CourierRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return courierRepository.completeDelivery()
    }
}
