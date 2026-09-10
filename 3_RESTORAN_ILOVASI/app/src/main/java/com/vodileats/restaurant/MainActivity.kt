package com.vodileats.restaurant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.vodileats.restaurant.data.RestaurantApi
import com.vodileats.restaurant.data.RestaurantPrefs
import com.vodileats.restaurant.theme.OrangePrimary
import com.vodileats.restaurant.theme.RestaurantTheme
import com.vodileats.restaurant.ui.RestaurantAuthScreen
import com.vodileats.restaurant.ui.RestaurantMenuScreen
import com.vodileats.restaurant.ui.RestaurantOrdersScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var api: RestaurantApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RestaurantTheme {
                val context = this@MainActivity
                var isLoggedIn by remember { mutableStateOf(RestaurantPrefs.isLoggedIn(context)) }
                var selectedTab by remember { mutableStateOf(0) }

                if (!isLoggedIn) {
                    RestaurantAuthScreen(
                        api = api,
                        onAuthSuccess = {
                            isLoggedIn = true
                        }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                                    label = { Text("Buyurtmalar") },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = OrangePrimary)
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                                    label = { Text("Menyu / Stop-list") },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = OrangePrimary)
                                )
                            }
                        }
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding)) {
                            when (selectedTab) {
                                0 -> RestaurantOrdersScreen(
                                    onLogout = {
                                        RestaurantPrefs.logout(context)
                                        isLoggedIn = false
                                    }
                                )
                                1 -> RestaurantMenuScreen(
                                    onLogout = {
                                        RestaurantPrefs.logout(context)
                                        isLoggedIn = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
