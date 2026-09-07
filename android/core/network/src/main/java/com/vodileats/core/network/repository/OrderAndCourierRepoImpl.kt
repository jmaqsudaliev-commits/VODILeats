package com.vodileats.core.network.repository

import com.vodileats.core.common.result.Resource
import com.vodileats.core.domain.model.*
import com.vodileats.core.domain.repository.CourierRepository
import com.vodileats.core.domain.repository.OrderRepository
import com.vodileats.core.network.api.CourierApi
import com.vodileats.core.network.api.OrderApi
import com.vodileats.core.network.model.OrderResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderApi: OrderApi
) : OrderRepository {

    private fun mapToDomain(dto: OrderResponse): Order {
        return Order(
            id = dto.id,
            orderNumber = dto.orderNumber,
            restaurantId = dto.restaurantId,
            restaurantName = dto.restaurant?.name ?: "",
            status = OrderStatus.valueOf(dto.status),
            totalAmount = dto.totalAmount,
            deliveryFee = dto.deliveryFee,
            deliveryAddress = dto.deliveryAddress,
            deliveryLatitude = dto.deliveryLatitude,
            deliveryLongitude = dto.deliveryLongitude,
            items = dto.items.map { itemDto ->
                OrderItem(
                    id = itemDto.id,
                    menuItemId = itemDto.menuItemId,
                    name = itemDto.name,
                    price = itemDto.price,
                    quantity = itemDto.quantity,
                    imageUrl = null
                )
            },
            courier = dto.courier?.let {
                CourierInfo(
                    id = it.id,
                    name = it.name,
                    phone = it.phone,
                    currentLat = it.latitude,
                    currentLng = it.longitude
                )
            },
            createdAt = dto.createdAt
        )
    }

    override suspend fun createOrder(
        restaurantId: String,
        deliveryAddress: String,
        deliveryLatitude: Double,
        deliveryLongitude: Double,
        paymentMethod: String,
        customerNote: String?,
        items: List<Map<String, Any>>
    ): Resource<Order> {
        return try {
            val body = mapOf(
                "restaurantId" to restaurantId,
                "deliveryAddress" to deliveryAddress,
                "deliveryLatitude" to deliveryLatitude,
                "deliveryLongitude" to deliveryLongitude,
                "paymentMethod" to paymentMethod,
                "customerNote" to (customerNote ?: ""),
                "items" to items
            )
            val response = orderApi.createOrder(body)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(mapToDomain(response.body()!!))
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Buyurtma berishda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun getMyOrders(page: Int): Resource<List<Order>> {
        return try {
            val response = orderApi.getMyOrders(page)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.data.map { mapToDomain(it) })
            } else {
                Resource.Error("Buyurtmalarni yuklashda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun getOrderDetail(id: String): Resource<Order> {
        return try {
            val response = orderApi.getOrder(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(mapToDomain(response.body()!!))
            } else {
                Resource.Error("Buyurtma topilmadi")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun cancelOrder(id: String, reason: String): Resource<Order> {
        return try {
            val response = orderApi.cancelOrder(id, mapOf("reason" to reason))
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(mapToDomain(response.body()!!))
            } else {
                Resource.Error("Buyurtmani bekor qilishda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun getActiveOrders(restaurantId: String): Resource<List<Order>> {
        return try {
            val response = orderApi.getActiveOrders(restaurantId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.map { mapToDomain(it) })
            } else {
                Resource.Error("Buyurtmalarni yuklashda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun confirmOrder(id: String): Resource<Order> {
        return try {
            val response = orderApi.confirmOrder(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(mapToDomain(response.body()!!))
            } else {
                Resource.Error("Tasdiqlashda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun startPreparing(id: String): Resource<Order> {
        return try {
            val response = orderApi.startPreparing(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(mapToDomain(response.body()!!))
            } else {
                Resource.Error("Holatni o'zgartirishda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun markReady(id: String): Resource<Order> {
        return try {
            val response = orderApi.markReady(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(mapToDomain(response.body()!!))
            } else {
                Resource.Error("Holatni o'zgartirishda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun markPickedUp(id: String): Resource<Order> {
        return try {
            val response = orderApi.markPickedUp(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(mapToDomain(response.body()!!))
            } else {
                Resource.Error("Holatni o'zgartirishda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun startDelivering(id: String): Resource<Order> {
        return try {
            val response = orderApi.startDelivering(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(mapToDomain(response.body()!!))
            } else {
                Resource.Error("Holatni o'zgartirishda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }

    override suspend fun markDelivered(id: String): Resource<Order> {
        return try {
            val response = orderApi.markDelivered(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(mapToDomain(response.body()!!))
            } else {
                Resource.Error("Holatni o'zgartirishda xatolik")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tarmoq xatosi")
        }
    }
}

@Singleton
class CourierRepositoryImpl @Inject constructor(
    private val courierApi: CourierApi
) : CourierRepository {

    override suspend fun updateStatus(status: String): Resource<Unit> {
        return try {
            val res = courierApi.updateStatus(mapOf("status" to status))
            if (res.isSuccessful) Resource.Success(Unit)
            else Resource.Error("Holat yangilanmadi")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Xatolik")
        }
    }

    override suspend fun updateLocation(latitude: Double, longitude: Double): Resource<Unit> {
        return try {
            val res = courierApi.updateLocation(mapOf("latitude" to latitude, "longitude" to longitude))
            if (res.isSuccessful) Resource.Success(Unit)
            else Resource.Error("Lokatsiya yangilanmadi")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Xatolik")
        }
    }

    override suspend fun acceptOrder(orderId: String): Resource<Unit> {
        return try {
            val res = courierApi.acceptOrder(mapOf("orderId" to orderId))
            if (res.isSuccessful) Resource.Success(Unit)
            else Resource.Error("Buyurtmani qabul qilib bo'lmadi")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Xatolik")
        }
    }

    override suspend fun rejectOrder(orderId: String): Resource<Unit> {
        return try {
            val res = courierApi.rejectOrder(mapOf("orderId" to orderId))
            if (res.isSuccessful) Resource.Success(Unit)
            else Resource.Error("Buyurtmani rad etib bo'lmadi")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Xatolik")
        }
    }

    override suspend fun completeDelivery(): Resource<Unit> {
        return try {
            val res = courierApi.completeDelivery()
            if (res.isSuccessful) Resource.Success(Unit)
            else Resource.Error("Yetkazishni yakunlab bo'lmadi")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Xatolik")
        }
    }
}
