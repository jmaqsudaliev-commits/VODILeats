package com.vodileats.feature.customer.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodileats.core.common.result.Resource
import com.vodileats.core.database.dao.CartDao
import com.vodileats.core.database.entity.CartItemEntity
import com.vodileats.core.domain.usecase.CreateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartState(
    val items: List<CartItemEntity> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 12000.0,
    val totalAmount: Double = 12000.0,
    val deliveryAddress: String = "Vodil markazi, Navoiy ko'chasi 45",
    val customerNote: String = "",
    val paymentMethod: String = "CASH", // CASH, PAYME, CLICK
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdOrderId: String? = null
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartDao: CartDao,
    private val createOrderUseCase: CreateOrderUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state.asStateFlow()

    init {
        observeCart()
    }

    private fun observeCart() {
        viewModelScope.launch {
            cartDao.getAllCartItems().collect { list ->
                val sub = list.sumOf { it.price * it.quantity }
                val fee = if (list.isEmpty()) 0.0 else 12000.0
                _state.update {
                    it.copy(
                        items = list,
                        subtotal = sub,
                        deliveryFee = fee,
                        totalAmount = sub + fee
                    )
                }
            }
        }
    }

    fun onAddressChange(address: String) {
        _state.update { it.copy(deliveryAddress = address) }
    }

    fun onNoteChange(note: String) {
        _state.update { it.copy(customerNote = note) }
    }

    fun onPaymentMethodChange(method: String) {
        _state.update { it.copy(paymentMethod = method) }
    }

    fun increaseQuantity(item: CartItemEntity) {
        viewModelScope.launch {
            cartDao.updateQuantity(item.menuItemId, item.quantity + 1)
        }
    }

    fun decreaseQuantity(item: CartItemEntity) {
        viewModelScope.launch {
            if (item.quantity > 1) {
                cartDao.updateQuantity(item.menuItemId, item.quantity - 1)
            } else {
                cartDao.deleteItem(item)
            }
        }
    }

    fun checkout(onSuccess: (String) -> Unit) {
        val st = _state.value
        if (st.items.isEmpty()) return

        val restaurantId = st.items.first().restaurantId
        val orderItems = st.items.map {
            mapOf(
                "menuItemId" to it.menuItemId,
                "name" to it.name,
                "price" to it.price,
                "quantity" to it.quantity
            )
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = createOrderUseCase(
                restaurantId = restaurantId,
                deliveryAddress = st.deliveryAddress,
                deliveryLatitude = 40.1772,
                deliveryLongitude = 71.7228,
                paymentMethod = st.paymentMethod,
                customerNote = st.customerNote,
                items = orderItems
            )
            when (result) {
                is Resource.Success -> {
                    val order = result.data
                    cartDao.clearCart()
                    _state.update { it.copy(isLoading = false, createdOrderId = order?.id) }
                    order?.id?.let { onSuccess(it) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }
}
