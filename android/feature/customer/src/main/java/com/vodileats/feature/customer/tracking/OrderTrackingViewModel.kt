package com.vodileats.feature.customer.tracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodileats.core.common.result.Resource
import com.vodileats.core.domain.model.CourierLocation
import com.vodileats.core.domain.model.Order
import com.vodileats.core.domain.model.OrderStatus
import com.vodileats.core.domain.usecase.GetOrderDetailUseCase
import com.vodileats.core.network.websocket.TrackingWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackingUiState(
    val order: Order? = null,
    val orderStatus: OrderStatus = OrderStatus.PENDING,
    val courierLocation: CourierLocation? = null,
    val previousCourierLocation: CourierLocation? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val estimatedMinutes: Int = 0
)

sealed class TrackingIntent {
    data class LoadOrder(val orderId: String) : TrackingIntent()
    data object StartTracking : TrackingIntent()
    data object StopTracking : TrackingIntent()
}

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val getOrderDetailUseCase: GetOrderDetailUseCase,
    private val webSocketClient: TrackingWebSocketClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId") ?: ""

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    init {
        if (orderId.isNotEmpty()) {
            processIntent(TrackingIntent.LoadOrder(orderId))
            processIntent(TrackingIntent.StartTracking)
        }
    }

    fun processIntent(intent: TrackingIntent) {
        when (intent) {
            is TrackingIntent.LoadOrder -> loadOrder(intent.orderId)
            is TrackingIntent.StartTracking -> startTracking()
            is TrackingIntent.StopTracking -> stopTracking()
        }
    }

    private fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = getOrderDetailUseCase(orderId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            order = result.data,
                            orderStatus = result.data.status,
                            isLoading = false,
                            error = null
                        )
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }

                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun startTracking() {
        // WebSocket orqali buyurtmani kuzatishni boshlash
        webSocketClient.trackOrder(orderId)

        // Kuryer lokatsiya yangilanishlarini tinglash
        viewModelScope.launch {
            webSocketClient.courierLocationUpdates.collect { update ->
                _uiState.update { state ->
                    state.copy(
                        previousCourierLocation = state.courierLocation,
                        courierLocation = CourierLocation(
                            courierId = update.courierId,
                            latitude = update.latitude,
                            longitude = update.longitude,
                            bearing = update.bearing,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        // Buyurtma status o'zgarishlarini tinglash
        viewModelScope.launch {
            webSocketClient.orderStatusChanges.collect { change ->
                if (change.orderId == orderId) {
                    val newStatus = OrderStatus.fromString(change.status)
                    _uiState.update { it.copy(orderStatus = newStatus) }

                    // Agar yetkazildi yoki bekor qilindi bo'lsa, qayta yuklash
                    if (newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.CANCELLED) {
                        loadOrder(orderId)
                    }
                }
            }
        }
    }

    private fun stopTracking() {
        // WebSocket kuzatishni to'xtatish (room'dan chiqish)
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
