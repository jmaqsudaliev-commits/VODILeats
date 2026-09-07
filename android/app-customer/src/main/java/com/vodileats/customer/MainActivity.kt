package com.vodileats.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vodileats.core.ui.theme.VodilEatsTheme
import com.vodileats.feature.auth.AuthScreen
import com.vodileats.feature.auth.AuthViewModel
import com.vodileats.feature.customer.cart.CartScreen
import com.vodileats.feature.customer.cart.CartViewModel
import com.vodileats.feature.customer.catalog.RestaurantCatalogScreen
import com.vodileats.feature.customer.catalog.RestaurantCatalogViewModel
import com.vodileats.feature.customer.detail.RestaurantDetailScreen
import com.vodileats.feature.customer.detail.RestaurantDetailViewModel
import com.vodileats.feature.customer.tracking.OrderTrackingScreen
import com.vodileats.feature.customer.tracking.OrderTrackingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VodilEatsTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "catalog"
                ) {
                    composable("auth") {
                        val authViewModel: AuthViewModel = hiltViewModel()
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = {
                                navController.navigate("catalog") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("catalog") {
                        val catalogViewModel: RestaurantCatalogViewModel = hiltViewModel()
                        RestaurantCatalogScreen(
                            viewModel = catalogViewModel,
                            onRestaurantClick = { id ->
                                navController.navigate("restaurant/$id")
                            },
                            onCartClick = {
                                navController.navigate("cart")
                            }
                        )
                    }

                    composable(
                        route = "restaurant/{restaurantId}",
                        arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
                    ) {
                        val detailViewModel: RestaurantDetailViewModel = hiltViewModel()
                        RestaurantDetailScreen(
                            viewModel = detailViewModel,
                            onBackClick = { navController.popBackStack() },
                            onNavigateToCart = { navController.navigate("cart") }
                        )
                    }

                    composable("cart") {
                        val cartViewModel: CartViewModel = hiltViewModel()
                        CartScreen(
                            viewModel = cartViewModel,
                            onBackClick = { navController.popBackStack() },
                            onOrderPlaced = { orderId ->
                                navController.navigate("tracking/$orderId") {
                                    popUpTo("catalog")
                                }
                            }
                        )
                    }

                    composable(
                        route = "tracking/{orderId}",
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) {
                        val trackingViewModel: OrderTrackingViewModel = hiltViewModel()
                        OrderTrackingScreen(
                            viewModel = trackingViewModel,
                            onBackClick = { navController.navigate("catalog") }
                        )
                    }
                }
            }
        }
    }
}
