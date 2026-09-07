package com.vodileats.core.domain.model

/**
 * Domain modellar - UI va Data qatlamlardan mustaqil
 */

data class Restaurant(
    val id: String,
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val coverImageUrl: String?,
    val rating: Double,
    val totalReviews: Int,
    val avgDeliveryTimeMinutes: Int,
    val minimumOrderAmount: Double,
    val deliveryFeeBase: Double,
    val categories: List<Category> = emptyList()
)

data class Category(
    val id: String,
    val name: String,
    val description: String?,
    val items: List<MenuItem> = emptyList()
)

data class MenuItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String?,
    val preparationTimeMinutes: Int,
    val calories: Int,
    val tags: List<String>,
    val isAvailable: Boolean,
    val isInStopList: Boolean
)

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int,
    val specialInstructions: String? = null
) {
    val totalPrice: Double get() = menuItem.price * quantity
}

data class Cart(
    val restaurantId: String,
    val restaurantName: String,
    val items: List<CartItem> = emptyList()
) {
    val subtotal: Double get() = items.sumOf { it.totalPrice }
    val itemCount: Int get() = items.sumOf { it.quantity }
    val isEmpty: Boolean get() = items.isEmpty()
}

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    COURIER_ASSIGNED,
    COURIER_PICKING_UP,
    COURIER_PICKED_UP,
    DELIVERING,
    DELIVERED,
    CANCELLED;

    val displayName: String
        get() = when (this) {
            PENDING -> "Kutilmoqda"
            CONFIRMED -> "Tasdiqlandi"
            PREPARING -> "Tayyorlanmoqda"
            READY_FOR_PICKUP -> "Kuryerga tayyor"
            COURIER_ASSIGNED -> "Kuryer tayinlandi"
            COURIER_PICKING_UP -> "Kuryer yo'lda (restoranga)"
            COURIER_PICKED_UP -> "Kuryer oldi"
            DELIVERING -> "Yetkazilmoqda"
            DELIVERED -> "Yetkazildi"
            CANCELLED -> "Bekor qilindi"
        }

    companion object {
        fun fromString(value: String): OrderStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

data class Order(
    val id: String,
    val orderNumber: String,
    val status: OrderStatus,
    val restaurantName: String,
    val deliveryAddress: String,
    val deliveryLatitude: Double,
    val deliveryLongitude: Double,
    val subtotal: Double,
    val deliveryFee: Double,
    val totalAmount: Double,
    val distanceKm: Double,
    val items: List<OrderItem>,
    val customerNote: String?,
    val estimatedDeliveryTime: String?,
    val createdAt: String
)

data class OrderItem(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val totalPrice: Double
)

data class CourierLocation(
    val courierId: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float,
    val timestamp: Long
)

data class UserProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val role: String,
    val isPhoneVerified: Boolean
)

data class DeliveryAddress(
    val id: String,
    val title: String,
    val street: String,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean
)
