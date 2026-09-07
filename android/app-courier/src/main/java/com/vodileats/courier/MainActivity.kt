package com.vodileats.courier

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.vodileats.core.ui.theme.VodilEatsTheme
import com.vodileats.feature.courier.delivery.ActiveDeliveryScreen
import com.vodileats.feature.courier.delivery.ActiveDeliveryViewModel
import com.vodileats.feature.courier.service.CourierLocationService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start GPS Foreground Tracking Service
        val serviceIntent = Intent(this, CourierLocationService::class.java).apply {
            action = CourierLocationService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            VodilEatsTheme {
                val deliveryViewModel: ActiveDeliveryViewModel = hiltViewModel()
                ActiveDeliveryScreen(viewModel = deliveryViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop service when app is destroyed if desired, or keep alive for background shifts
    }
}
