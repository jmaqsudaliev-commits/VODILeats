package com.vodileats.courier

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.vodileats.courier.data.CourierApi
import com.vodileats.courier.data.CourierPrefs
import com.vodileats.courier.service.CourierLocationService
import com.vodileats.courier.theme.CourierTheme
import com.vodileats.courier.ui.CourierAuthScreen
import com.vodileats.courier.ui.CourierDashboardScreen
import com.vodileats.courier.ui.CourierPendingScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var api: CourierApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CourierTheme {
                val context = this@MainActivity

                // Determine initial screen state based on CourierPrefs
                var currentScreen by remember {
                    mutableStateOf(
                        when {
                            !CourierPrefs.isLoggedIn(context) -> "AUTH"
                            !CourierPrefs.isVerified(context) -> "PENDING"
                            else -> "DASHBOARD"
                        }
                    )
                }

                // Start location service when on DASHBOARD
                LaunchedEffect(currentScreen) {
                    if (currentScreen == "DASHBOARD") {
                        val serviceIntent = Intent(context, CourierLocationService::class.java).apply {
                            action = CourierLocationService.ACTION_START
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(context, serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }

                when (currentScreen) {
                    "AUTH" -> {
                        CourierAuthScreen(
                            api = api,
                            onAuthSuccess = { isVerified ->
                                currentScreen = if (isVerified) "DASHBOARD" else "PENDING"
                            }
                        )
                    }
                    "PENDING" -> {
                        CourierPendingScreen(
                            api = api,
                            onApproved = {
                                currentScreen = "DASHBOARD"
                            },
                            onLogout = {
                                CourierPrefs.logout(context)
                                currentScreen = "AUTH"
                            }
                        )
                    }
                    "DASHBOARD" -> {
                        CourierDashboardScreen(
                            onLogout = {
                                val serviceIntent = Intent(context, CourierLocationService::class.java).apply {
                                    action = CourierLocationService.ACTION_STOP
                                }
                                context.stopService(serviceIntent)
                                CourierPrefs.logout(context)
                                currentScreen = "AUTH"
                            }
                        )
                    }
                }
            }
        }
    }
}
