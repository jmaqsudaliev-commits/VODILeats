package com.vodileats.courier.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vodileats.courier.theme.*

enum class CourierStep {
    TO_RESTAURANT,
    PICKUP_ORDER,
    TO_CUSTOMER,
    COMPLETED
}

@Composable
fun CourierDashboardScreen() {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(true) }
    var currentStep by remember { mutableStateOf(CourierStep.TO_RESTAURANT) }

    val courierPos = LatLng(40.1772, 71.7228)
    val restaurantPos = LatLng(40.1795, 71.7250)
    val customerPos = LatLng(40.1830, 71.7290)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(courierPos, 15f)
    }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Google Maps
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                Marker(state = MarkerState(position = courierPos), title = "Siz")
                Marker(state = MarkerState(position = restaurantPos), title = "Shohona Milliy Taomlar")
                Marker(state = MarkerState(position = customerPos), title = "Mijoz: Vodil 14-uy")

                val target = if (currentStep == CourierStep.TO_RESTAURANT || currentStep == CourierStep.PICKUP_ORDER) {
                    restaurantPos
                } else {
                    customerPos
                }

                Polyline(points = listOf(courierPos, target), color = OrangePrimary, width = 12f)
            }

            // Top Status Bar: Online/Offline Switch
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(24.dp),
                color = if (isOnline) FreshGreen else Color(0xFF475569),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOnline) "ONLINE • Buyurtmalar ochiq" else "OFFLINE • Dam olishda",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { isOnline = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FreshGreen,
                            checkedTrackColor = Color.White,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.White
                        )
                    )
                }
            }

            // Bottom Action Card
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                    val stepTitle = when (currentStep) {
                        CourierStep.TO_RESTAURANT -> "1-QADAM: Restoranga yo'l oling"
                        CourierStep.PICKUP_ORDER -> "2-QADAM: Buyurtmani qabul qiling"
                        CourierStep.TO_CUSTOMER -> "3-QADAM: Mijozga yetkazing"
                        CourierStep.COMPLETED -> "Buyurtma yetkazildi! Yangisi kutilmoqda..."
                    }

                    val address = when (currentStep) {
                        CourierStep.TO_RESTAURANT, CourierStep.PICKUP_ORDER -> "Shohona Milliy Taomlar • Mustaqillik shoh ko'chasi 14"
                        CourierStep.TO_CUSTOMER -> "Vodil markazi, 14-uy • Azizbek Rahimov"
                        CourierStep.COMPLETED -> "Yangi buyurtmalar kutilmoqda..."
                    }

                    Text(stepTitle, color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(address, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+998901234567"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = FreshGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigator")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (currentStep) {
                        CourierStep.TO_RESTAURANT -> {
                            Button(
                                onClick = { currentStep = CourierStep.PICKUP_ORDER },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                            ) {
                                Text("Restoranga yetib keldim", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        CourierStep.PICKUP_ORDER -> {
                            Button(
                                onClick = { currentStep = CourierStep.TO_CUSTOMER },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FreshGreen)
                            ) {
                                Text("Buyurtmani oldim (Mijoz tomon yo'lga)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        CourierStep.TO_CUSTOMER -> {
                            Button(
                                onClick = { currentStep = CourierStep.COMPLETED },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                            ) {
                                Text("Mijozga topshirdim (Yetkazildi)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        CourierStep.COMPLETED -> {
                            Button(
                                onClick = { currentStep = CourierStep.TO_RESTAURANT },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FreshGreen)
                            ) {
                                Text("Yangi buyurtmani qabul qilish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
