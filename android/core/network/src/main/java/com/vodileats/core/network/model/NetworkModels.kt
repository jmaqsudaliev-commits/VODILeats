package com.vodileats.core.network.model

import com.google.gson.annotations.SerializedName

data class PaginatedResponse<T>(
    val data: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int
)

data class RestaurantResponse(
    val id: String,
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val coverImageUrl: String?,
    val phone: String,
    val rating: Double,
    val totalReviews: Int,
    val avgDeliveryTimeMinutes: Int,
    val minimumOrderAmount: Double,
    val deliveryFeeBase: Double,
    val deliveryFeePerKm: Double,
    val openTime: String,
    val closeTime: String,
    val isActive: Boolean,
    val categories: List<CategoryResponse>?
)

data class CategoryResponse(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val sortOrder: Int,
    val items: List<MenuItemResponse>?
)

data class MenuItemResponse(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String?,
    val preparationTimeMinutes: Int,
    val calories: Int,
    val tags: List<String>?,
    val isAvailable: Boolean,
    val isInStopList: Boolean
)

data class DeliveryFeeResponse(
    val distanceKm: Double,
    val deliveryFee: Double
)

data class OrderResponse(
    val id: String,
    val orderNumber: String,
    val status: String,
    val paymentMethod: String,
    val customerId: String,
    val restaurantId: String,
    val courierId: String?,
    val deliveryAddress: String,
    val deliveryLatitude: Double,
    val deliveryLongitude: Double,
    val subtotal: Double,
    val deliveryFee: Double,
    val totalAmount: Double,
    val distanceKm: Double,
    val estimatedDeliveryTime: String?,
    val customerNote: String?,
    val items: List<OrderItemResponse>,
    val restaurant: RestaurantResponse?,
    val createdAt: String,
    val updatedAt: String
)

data class OrderItemResponse(
    val id: String,
    val menuItemId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val totalPrice: Double,
    val specialInstructions: String?
)

data class CourierLocationUpdate(
    val courierId: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float,
    val timestamp: String
)

data class OrderOfferData(
    val orderId: String,
    val orderNumber: String,
    val restaurantName: String,
    val restaurantAddress: String,
    val restaurantLatitude: Double,
    val restaurantLongitude: Double,
    val deliveryAddress: String,
    val deliveryLatitude: Double,
    val deliveryLongitude: Double,
    val totalAmount: Double,
    val distanceToRestaurant: Double,
    val estimatedDeliveryDistance: Double,
    val timeoutSeconds: Int
)
