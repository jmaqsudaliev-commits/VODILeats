package com.vodileats.feature.courier.delivery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vodileats.core.ui.component.VodilButton
import com.vodileats.core.ui.theme.*

@Composable
fun ActiveDeliveryScreen(
    viewModel: ActiveDeliveryViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val courierPos = LatLng(state.courierLat, state.courierLng)
    val restaurantPos = LatLng(state.restaurantLat, state.restaurantLng)
    val customerPos = LatLng(state.customerLat, state.customerLng)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(courierPos, 15f)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Google Maps
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = true
                )
            ) {
                // Courier Marker
                Marker(
                    state = MarkerState(position = courierPos),
                    title = "Sizning joylashuvingiz"
                )

                // Restaurant Marker
                Marker(
                    state = MarkerState(position = restaurantPos),
                    title = "Restoran: Shohona Milliy Taomlar"
                )

                // Customer Marker
                Marker(
                    state = MarkerState(position = customerPos),
                    title = "Mijoz: Vodil markazi, 12-uy"
                )

                // Route Polyline
                val targetPos = if (state.currentStep == DeliveryStep.TO_RESTAURANT || state.currentStep == DeliveryStep.PICKUP_ORDER) {
                    restaurantPos
                } else {
                    customerPos
                }

                Polyline(
                    points = listOf(courierPos, targetPos),
                    color = OrangePrimary,
                    width = 12f
                )
            }

            // Top Status Bar: Online/Offline Toggle
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(24.dp),
                color = if (state.isOnline) FreshGreen else Gray700,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isOnline) "ONLINE • Buyurtmalar qabul qilinmoqda" else "OFFLINE • Dam olishda",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = state.isOnline,
                        onCheckedChange = { viewModel.toggleOnline(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FreshGreen,
                            checkedTrackColor = Color.White,
                            uncheckedThumbColor = Gray700,
                            uncheckedTrackColor = Color.White
                        )
                    )
                }
            }

            // Bottom Delivery Action Card
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .navigationBarsPadding()
                ) {
                    // Header Status
                    val stepTitle = when (state.currentStep) {
                        DeliveryStep.TO_RESTAURANT -> "1-QADAM: Restoranga yo'l oling"
                        DeliveryStep.PICKUP_ORDER -> "2-QADAM: Buyurtmani qabul qiling"
                        DeliveryStep.TO_CUSTOMER -> "3-QADAM: Mijozga yetkazing"
                        DeliveryStep.COMPLETED -> "Yangi buyurtmalar kutilmoqda..."
                    }

                    val targetAddress = when (state.currentStep) {
                        DeliveryStep.TO_RESTAURANT, DeliveryStep.PICKUP_ORDER -> "Shohona Taomlar • Mustaqillik ko'chasi 4"
                        DeliveryStep.TO_CUSTOMER -> "Vodil markazi, 12-uy • Azizbek Rahimov"
                        DeliveryStep.COMPLETED -> "Hozircha faol buyurtma yo'q"
                    }

                    Text(
                        text = stepTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = OrangePrimary,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = targetAddress,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons (Call & Navigate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+998901234567"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Qo'ng'iroq")
                        }

                        OutlinedButton(
                            onClick = {
                                val gmmIntentUri = Uri.parse("geo:0,0?q=40.1795,71.7250(Restoran)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = FreshGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigator")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step Action Button
                    when (state.currentStep) {
                        DeliveryStep.TO_RESTAURANT -> {
                            VodilButton(
                                text = "Restoranga yetib keldim",
                                onClick = { viewModel.onArrivedAtRestaurant() }
                            )
                        }
                        DeliveryStep.PICKUP_ORDER -> {
                            VodilButton(
                                text = "Buyurtmani oldim (Yo'lga chiqish)",
                                onClick = { viewModel.onOrderPickedUp("demo-order-id") },
                                containerColor = FreshGreen
                            )
                        }
                        DeliveryStep.TO_CUSTOMER -> {
                            VodilButton(
                                text = "Mijozga topshirdim (Yetkazildi)",
                                onClick = { viewModel.onDelivered("demo-order-id") },
                                containerColor = OrangePrimary
                            )
                        }
                        DeliveryStep.COMPLETED -> {
                            Text(
                                text = "Navbatdagi buyurtma tez orada keladi...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray500
                            )
                        }
                    }
                }
            }
        }
    }
}
