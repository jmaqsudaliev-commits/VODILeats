package com.vodileats.restaurant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vodileats.core.ui.theme.OrangePrimary
import com.vodileats.core.ui.theme.VodilEatsTheme
import com.vodileats.feature.restaurant.menu.MenuManagementScreen
import com.vodileats.feature.restaurant.menu.MenuManagementViewModel
import com.vodileats.feature.restaurant.orders.RestaurantOrdersScreen
import com.vodileats.feature.restaurant.orders.RestaurantOrdersViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VodilEatsTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == "orders",
                                onClick = {
                                    navController.navigate("orders") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                                label = { Text("Buyurtmalar") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = OrangePrimary)
                            )

                            NavigationBarItem(
                                selected = currentRoute == "menu",
                                onClick = {
                                    navController.navigate("menu") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                                label = { Text("Menyu") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = OrangePrimary)
                            )
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "orders",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("orders") {
                            val ordersViewModel: RestaurantOrdersViewModel = hiltViewModel()
                            RestaurantOrdersScreen(viewModel = ordersViewModel)
                        }

                        composable("menu") {
                            val menuViewModel: MenuManagementViewModel = hiltViewModel()
                            MenuManagementScreen(
                                viewModel = menuViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
