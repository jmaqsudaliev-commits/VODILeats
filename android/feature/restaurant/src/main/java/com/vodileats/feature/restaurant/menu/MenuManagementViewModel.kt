package com.vodileats.feature.restaurant.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodileats.core.common.result.Resource
import com.vodileats.core.domain.model.Category
import com.vodileats.core.domain.model.MenuItem
import com.vodileats.core.domain.usecase.GetMenuUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuManagementState(
    val restaurantId: String = "demo-restaurant-id",
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class MenuManagementViewModel @Inject constructor(
    private val getMenuUseCase: GetMenuUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MenuManagementState())
    val state: StateFlow<MenuManagementState> = _state.asStateFlow()

    init {
        loadMenu()
    }

    fun loadMenu() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getMenuUseCase(_state.value.restaurantId)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            categories = result.data ?: emptyList(),
                            isLoading = false
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

    fun onSearchChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleAvailability(categoryIndex: Int, itemIndex: Int) {
        val currentCats = _state.value.categories.toMutableList()
        val cat = currentCats[categoryIndex]
        val currentItems = cat.items.toMutableList()
        val item = currentItems[itemIndex]

        val updatedItem = item.copy(isAvailable = !item.isAvailable)
        currentItems[itemIndex] = updatedItem
        currentCats[categoryIndex] = cat.copy(items = currentItems)

        _state.update { it.copy(categories = currentCats) }
        // Sync with backend API in background
    }
}
