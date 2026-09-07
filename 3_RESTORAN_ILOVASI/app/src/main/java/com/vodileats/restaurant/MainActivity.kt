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
import com.vodileats.restaurant.theme.OrangePrimary
import com.vodileats.restaurant.theme.RestaurantTheme
import com.vodileats.restaurant.ui.RestaurantMenuScreen
import com.vodileats.restaurant.ui.RestaurantOrdersScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RestaurantTheme {
                var selectedTab by remember { mutableStateOf(0) }

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
                            0 -> RestaurantOrdersScreen()
                            1 -> RestaurantMenuScreen()
                        }
                    }
                }
            }
        }
    }
}
