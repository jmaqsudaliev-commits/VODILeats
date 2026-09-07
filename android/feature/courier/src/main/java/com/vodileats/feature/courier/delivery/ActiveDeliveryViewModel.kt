package com.vodileats.feature.courier.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodileats.core.common.result.Resource
import com.vodileats.core.domain.model.Order
import com.vodileats.core.domain.model.OrderStatus
import com.vodileats.core.domain.repository.CourierRepository
import com.vodileats.core.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DeliveryStep {
    TO_RESTAURANT,
    PICKUP_ORDER,
    TO_CUSTOMER,
    COMPLETED
}

data class ActiveDeliveryState(
    val isOnline: Boolean = true,
    val activeOrder: Order? = null,
    val currentStep: DeliveryStep = DeliveryStep.TO_RESTAURANT,
    val courierLat: Double = 40.1772,
    val courierLng: Double = 71.7228,
    val restaurantLat: Double = 40.1795,
    val restaurantLng: Double = 71.7250,
    val customerLat: Double = 40.1830,
    val customerLng: Double = 71.7290,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ActiveDeliveryViewModel @Inject constructor(
    private val courierRepository: CourierRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveDeliveryState())
    val state: StateFlow<ActiveDeliveryState> = _state.asStateFlow()

    fun toggleOnline(online: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isOnline = online) }
            val status = if (online) "ONLINE" else "OFFLINE"
            courierRepository.updateStatus(status)
        }
    }

    fun onArrivedAtRestaurant() {
        _state.update { it.copy(currentStep = DeliveryStep.PICKUP_ORDER) }
    }

    fun onOrderPickedUp(orderId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val res = orderRepository.markPickedUp(orderId)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentStep = DeliveryStep.TO_CUSTOMER
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = res.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun onDelivered(orderId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val res = orderRepository.markDelivered(orderId)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentStep = DeliveryStep.COMPLETED,
                            activeOrder = null
                        )
                    }
                    courierRepository.completeDelivery()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = res.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }
}
