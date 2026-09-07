package com.vodileats.feature.customer.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodileats.core.common.result.Resource
import com.vodileats.core.database.dao.CartDao
import com.vodileats.core.database.entity.CartItemEntity
import com.vodileats.core.domain.model.Category
import com.vodileats.core.domain.model.MenuItem
import com.vodileats.core.domain.model.Restaurant
import com.vodileats.core.domain.usecase.GetMenuUseCase
import com.vodileats.core.domain.usecase.GetRestaurantDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantDetailState(
    val restaurant: Restaurant? = null,
    val categories: List<Category> = emptyList(),
    val cartItems: Map<String, CartItemEntity> = emptyMap(), // key: menuItemId
    val totalCartAmount: Double = 0.0,
    val totalCartCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRestaurantDetailUseCase: GetRestaurantDetailUseCase,
    private val getMenuUseCase: GetMenuUseCase,
    private val cartDao: CartDao
) : ViewModel() {

    private val restaurantId: String = checkNotNull(savedStateHandle["restaurantId"])

    private val _state = MutableStateFlow(RestaurantDetailState())
    val state: StateFlow<RestaurantDetailState> = _state.asStateFlow()

    init {
        loadData()
        observeCart()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val restResult = getRestaurantDetailUseCase(restaurantId)
            val menuResult = getMenuUseCase(restaurantId)

            if (restResult is Resource.Success && menuResult is Resource.Success) {
                _state.update {
                    it.copy(
                        restaurant = restResult.data,
                        categories = menuResult.data ?: emptyList(),
                        isLoading = false
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = restResult.message ?: menuResult.message ?: "Xatolik yuz berdi"
                    )
                }
            }
        }
    }

    private fun observeCart() {
        viewModelScope.launch {
            cartDao.getAllCartItems().collect { items ->
                val map = items.associateBy { it.menuItemId }
                val totalAmount = items.sumOf { it.price * it.quantity }
                val totalCount = items.sumOf { it.quantity }
                _state.update {
                    it.copy(
                        cartItems = map,
                        totalCartAmount = totalAmount,
                        totalCartCount = totalCount
                    )
                }
            }
        }
    }

    fun addItem(item: MenuItem) {
        viewModelScope.launch {
            val existing = _state.value.cartItems[item.id]
            val restaurantName = _state.value.restaurant?.name ?: ""
            if (existing != null) {
                cartDao.updateQuantity(item.id, existing.quantity + 1)
            } else {
                cartDao.insertOrUpdateItem(
                    CartItemEntity(
                        menuItemId = item.id,
                        restaurantId = restaurantId,
                        restaurantName = restaurantName,
                        name = item.name,
                        price = item.price,
                        quantity = 1,
                        imageUrl = item.imageUrl
                    )
                )
            }
        }
    }

    fun removeItem(item: MenuItem) {
        viewModelScope.launch {
            val existing = _state.value.cartItems[item.id] ?: return@launch
            if (existing.quantity > 1) {
                cartDao.updateQuantity(item.id, existing.quantity - 1)
            } else {
                cartDao.deleteItemById(item.id)
            }
        }
    }
}
