package com.vodileats.feature.restaurant.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodileats.core.common.result.Resource
import com.vodileats.core.domain.model.Order
import com.vodileats.core.domain.model.OrderStatus
import com.vodileats.core.domain.usecase.ConfirmOrderUseCase
import com.vodileats.core.domain.usecase.GetActiveOrdersUseCase
import com.vodileats.core.domain.usecase.MarkOrderReadyUseCase
import com.vodileats.core.network.websocket.TrackingWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantOrdersUiState(
    val activeOrders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val newOrderAlert: Boolean = false
)

@HiltViewModel
class RestaurantOrdersViewModel @Inject constructor(
    private val getActiveOrdersUseCase: GetActiveOrdersUseCase,
    private val confirmOrderUseCase: ConfirmOrderUseCase,
    private val markOrderReadyUseCase: MarkOrderReadyUseCase,
    private val webSocketClient: TrackingWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantOrdersUiState())
    val uiState: StateFlow<RestaurantOrdersUiState> = _uiState.asStateFlow()

    // TODO: restaurantId'ni auth state'dan olish
    private var restaurantId: String = ""

    init {
        listenForNewOrders()
        listenForStatusChanges()
    }

    fun loadOrders(restaurantId: String) {
        this.restaurantId = restaurantId
        webSocketClient.listenRestaurant(restaurantId)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getActiveOrdersUseCase(restaurantId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(activeOrders = result.data, isLoading = false)
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(error = result.message, isLoading = false)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun listenForNewOrders() {
        viewModelScope.launch {
            webSocketClient.newOrderAlerts.collect { alert ->
                _uiState.update { it.copy(newOrderAlert = true) }
                // Buyurtmalar ro'yxatini qayta yuklash
                if (restaurantId.isNotEmpty()) {
                    loadOrders(restaurantId)
                }
            }
        }
    }

    private fun listenForStatusChanges() {
        viewModelScope.launch {
            webSocketClient.orderStatusChanges.collect { change ->
                _uiState.update { state ->
                    val updatedOrders = state.activeOrders.map { order ->
                        if (order.id == change.orderId) {
                            order.copy(status = OrderStatus.fromString(change.status))
                        } else {
                            order
                        }
                    }
                    state.copy(activeOrders = updatedOrders)
                }
            }
        }
    }

    fun confirmOrder(orderId: String) {
        viewModelScope.launch {
            confirmOrderUseCase(orderId)
            if (restaurantId.isNotEmpty()) loadOrders(restaurantId)
        }
    }

    fun startPreparing(orderId: String) {
        viewModelScope.launch {
            // Use OrderRepository directly or create another use case
            if (restaurantId.isNotEmpty()) loadOrders(restaurantId)
        }
    }

    fun markReady(orderId: String) {
        viewModelScope.launch {
            markOrderReadyUseCase(orderId)
            if (restaurantId.isNotEmpty()) loadOrders(restaurantId)
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            if (restaurantId.isNotEmpty()) loadOrders(restaurantId)
        }
    }

    fun dismissNewOrderAlert() {
        _uiState.update { it.copy(newOrderAlert = false) }
    }
}
