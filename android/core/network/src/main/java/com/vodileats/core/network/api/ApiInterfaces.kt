package com.vodileats.core.network.api

import com.vodileats.core.network.model.CategoryResponse
import com.vodileats.core.network.model.DeliveryFeeResponse
import com.vodileats.core.network.model.OrderResponse
import com.vodileats.core.network.model.PaginatedResponse
import com.vodileats.core.network.model.RestaurantResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RestaurantApi {

    @GET("restaurants")
    suspend fun getRestaurants(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("maxDistanceKm") maxDistanceKm: Double? = null
    ): Response<PaginatedResponse<RestaurantResponse>>

    @GET("restaurants/{id}")
    suspend fun getRestaurantDetail(
        @Path("id") id: String
    ): Response<RestaurantResponse>

    @GET("restaurants/{id}/delivery-fee")
    suspend fun getDeliveryFee(
        @Path("id") id: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): Response<DeliveryFeeResponse>

    @GET("menu/restaurant/{restaurantId}")
    suspend fun getMenu(
        @Path("restaurantId") restaurantId: String
    ): Response<List<CategoryResponse>>
}

interface OrderApi {

    @POST("orders")
    suspend fun createOrder(
        @Body body: Map<String, Any>
    ): Response<OrderResponse>

    @GET("orders/my-orders")
    suspend fun getMyOrders(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedResponse<OrderResponse>>

    @GET("orders/{id}")
    suspend fun getOrderDetail(
        @Path("id") id: String
    ): Response<OrderResponse>

    @PUT("orders/{id}/cancel")
    suspend fun cancelOrder(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<OrderResponse>

    // Restoran endpointlari
    @GET("orders/restaurant/{restaurantId}/active")
    suspend fun getActiveOrders(
        @Path("restaurantId") restaurantId: String
    ): Response<List<OrderResponse>>

    @PUT("orders/{id}/confirm")
    suspend fun confirmOrder(@Path("id") id: String): Response<OrderResponse>

    @PUT("orders/{id}/preparing")
    suspend fun startPreparing(@Path("id") id: String): Response<OrderResponse>

    @PUT("orders/{id}/ready")
    suspend fun markReady(@Path("id") id: String): Response<OrderResponse>

    // Kuryer endpointlari
    @PUT("orders/{id}/picked-up")
    suspend fun markPickedUp(@Path("id") id: String): Response<OrderResponse>

    @PUT("orders/{id}/delivering")
    suspend fun startDelivering(@Path("id") id: String): Response<OrderResponse>

    @PUT("orders/{id}/delivered")
    suspend fun markDelivered(@Path("id") id: String): Response<OrderResponse>
}

interface CourierApi {

    @POST("courier/profile")
    suspend fun createProfile(
        @Body body: Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("courier/profile")
    suspend fun getProfile(): Response<Map<String, Any>>

    @PUT("courier/status")
    suspend fun updateStatus(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @PUT("courier/location")
    suspend fun updateLocation(
        @Body body: Map<String, Double>
    ): Response<Map<String, String>>

    @PUT("courier/orders/{orderId}/accept")
    suspend fun acceptOrder(
        @Path("orderId") orderId: String
    ): Response<Map<String, Any>>

    @PUT("courier/orders/{orderId}/reject")
    suspend fun rejectOrder(
        @Path("orderId") orderId: String
    ): Response<Map<String, String>>

    @PUT("courier/delivery/complete")
    suspend fun completeDelivery(): Response<Map<String, Any>>
}
