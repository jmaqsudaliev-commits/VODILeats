package com.vodileats.feature.customer.catalog

import com.vodileats.core.domain.model.Restaurant

data class CatalogState(
    val restaurants: List<Restaurant> = emptyList(),
    val filteredRestaurants: List<Restaurant> = emptyList(),
    val selectedCategory: String = "Barchasi",
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val cartItemCount: Int = 0
)

sealed interface CatalogIntent {
    data class Search(val query: String) : CatalogIntent
    data class SelectCategory(val category: String) : CatalogIntent
    object Refresh : CatalogIntent
}
