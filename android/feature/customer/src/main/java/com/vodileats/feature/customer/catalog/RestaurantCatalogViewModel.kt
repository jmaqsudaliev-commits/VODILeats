package com.vodileats.feature.customer.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodileats.core.common.result.Resource
import com.vodileats.core.database.dao.CartDao
import com.vodileats.core.domain.usecase.GetRestaurantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RestaurantCatalogViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val cartDao: CartDao
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    init {
        loadRestaurants()
        observeCart()
    }

    private fun observeCart() {
        viewModelScope.launch {
            cartDao.getCartCount().collect { count ->
                _state.update { it.copy(cartItemCount = count) }
            }
        }
    }

    fun onIntent(intent: CatalogIntent) {
        when (intent) {
            is CatalogIntent.Search -> {
                _state.update { it.copy(searchQuery = intent.query) }
                filterRestaurants()
            }
            is CatalogIntent.SelectCategory -> {
                _state.update { it.copy(selectedCategory = intent.category) }
                filterRestaurants()
            }
            CatalogIntent.Refresh -> loadRestaurants()
        }
    }

    private fun loadRestaurants() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getRestaurantsUseCase(search = null)) {
                is Resource.Success -> {
                    val list = result.data ?: emptyList()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            restaurants = list,
                            filteredRestaurants = list
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun filterRestaurants() {
        val q = _state.value.searchQuery.trim().lowercase()
        val cat = _state.value.selectedCategory

        val filtered = _state.value.restaurants.filter { r ->
            val matchesQuery = q.isEmpty() || r.name.lowercase().contains(q) || r.description.lowercase().contains(q)
            val matchesCategory = (cat == "Barchasi") // in full app could check category tags
            matchesQuery && matchesCategory
        }
        _state.update { it.copy(filteredRestaurants = filtered) }
    }
}
