package com.vodileats.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vodileats.customer.data.CustomerApi
import com.vodileats.customer.theme.CustomerTheme
import com.vodileats.customer.ui.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var api: CustomerApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomerTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "auth") {
                    // 1. Auth Screen: Login via Phone + Password OR Register via Phone + SMS OTP + Name + Password
                    composable("auth") {
                        CustomerAuthScreen(
                            api = api,
                            onAuthSuccess = {
                                navController.navigate("catalog") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 2. Main Catalog: Restaurants, categories, search, banner
                    composable("catalog") {
                        CatalogScreen(
                            onRestaurantClick = { id -> navController.navigate("restaurant/$id") },
                            onCartClick = { navController.navigate("cart") }
                        )
                    }

                    // 3. Restaurant Menu Details: dishes, add/remove stepper
                    composable(
                        route = "restaurant/{restaurantId}",
                        arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("restaurantId") ?: ""
                        RestaurantDetailScreen(
                            restaurantId = id,
                            onBack = { navController.popBackStack() },
                            onNavigateToCart = { navController.navigate("cart") }
                        )
                    }

                    // 4. Cart & Checkout: address, note, payment methods (Cash, Payme, Click)
                    composable("cart") {
                        CartScreen(
                            onBack = { navController.popBackStack() },
                            onOrderPlaced = { orderId -> navController.navigate("tracking/$orderId") }
                        )
                    }

                    // 5. Live Tracking Screen: Google Maps with courier marker interpolation & timeline
                    composable(
                        route = "tracking/{orderId}",
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                        TrackingScreen(
                            orderId = orderId,
                            onBack = { navController.navigate("catalog") }
                        )
                    }
                }
            }
        }
    }
}
