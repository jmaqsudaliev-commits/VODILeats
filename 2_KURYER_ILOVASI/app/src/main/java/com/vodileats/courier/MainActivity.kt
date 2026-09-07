package com.vodileats.courier

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.vodileats.courier.service.CourierLocationService
import com.vodileats.courier.theme.CourierTheme
import com.vodileats.courier.ui.CourierDashboardScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start GPS Foreground Tracking Service in background
        val serviceIntent = Intent(this, CourierLocationService::class.java).apply {
            action = CourierLocationService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            CourierTheme {
                CourierDashboardScreen()
            }
        }
    }
}
