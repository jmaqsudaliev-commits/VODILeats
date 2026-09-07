package com.vodileats.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey
    val menuItemId: String,
    val restaurantId: String,
    val restaurantName: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String?,
    val specialInstructions: String? = null
)

@Entity(tableName = "cached_restaurants")
data class CachedRestaurantEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val deliveryFee: Double,
    val estimatedDeliveryMinutes: Int,
    val imageUrl: String,
    val isOpen: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)
