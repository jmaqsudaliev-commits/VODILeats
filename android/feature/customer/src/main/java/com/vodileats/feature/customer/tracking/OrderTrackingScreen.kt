package com.vodileats.feature.customer.tracking

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.vodileats.core.domain.model.CourierLocation
import com.vodileats.core.domain.model.OrderStatus

/**
 * BUYURTMA TRACKING EKRANI
 *
 * Bu ekran mijozga kuryer harakatini xaritada real-vaqtda ko'rsatadi.
 * Kuryer markeri silliq (interpolatsiya) bilan harakat qiladi.
 *
 * Asosiy xususiyatlar:
 * - Google Maps bilan Jetpack Compose integratsiyasi
 * - Kuryer markerining silliq animatsiyasi (interpolation + bearing)
 * - Buyurtma status timeline
 * - Taxminiy yetkazish vaqti
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Kuryer markerining animatsiya holati
    var animatedLat by remember { mutableFloatStateOf(0f) }
    var animatedLng by remember { mutableFloatStateOf(0f) }
    var animatedBearing by remember { mutableFloatStateOf(0f) }

    // Silliq marker animatsiyasi - interpolation
    LaunchedEffect(uiState.courierLocation) {
        val newLocation = uiState.courierLocation ?: return@LaunchedEffect
        val prevLocation = uiState.previousCourierLocation

        if (prevLocation == null) {
            // Birinchi lokatsiya - to'g'ridan-to'g'ri joylashtirish
            animatedLat = newLocation.latitude.toFloat()
            animatedLng = newLocation.longitude.toFloat()
            animatedBearing = newLocation.bearing
            return@LaunchedEffect
        }

        // Bearing interpolatsiyasi (eng qisqa yo'nalish)
        val startBearing = prevLocation.bearing
        val endBearing = newLocation.bearing
        var bearingDiff = endBearing - startBearing
        if (bearingDiff > 180) bearingDiff -= 360
        if (bearingDiff < -180) bearingDiff += 360

        // ValueAnimator bilan silliq o'tish (5 soniya ichida)
        val latAnimator = ValueAnimator.ofFloat(
            prevLocation.latitude.toFloat(),
            newLocation.latitude.toFloat()
        ).apply {
            duration = 4000 // 4 soniya (keyingi update 5 soniyada keladi)
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                animatedLat = animation.animatedValue as Float
            }
        }

        val lngAnimator = ValueAnimator.ofFloat(
            prevLocation.longitude.toFloat(),
            newLocation.longitude.toFloat()
        ).apply {
            duration = 4000
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                animatedLng = animation.animatedValue as Float
            }
        }

        val bearingAnimator = ValueAnimator.ofFloat(
            startBearing,
            startBearing + bearingDiff
        ).apply {
            duration = 1000
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                animatedBearing = animation.animatedValue as Float
            }
        }

        latAnimator.start()
        lngAnimator.start()
        bearingAnimator.start()
    }

    // Kamera holati
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(41.2995, 69.2401), // Default: Toshkent
            14f
        )
    }

    // Kamerani yangilash
    LaunchedEffect(animatedLat, animatedLng) {
        if (animatedLat != 0f && animatedLng != 0f) {
            val courierLatLng = LatLng(animatedLat.toDouble(), animatedLng.toDouble())

            // Buyurtma manzili va kuryer orasidagi boundni ko'rsatish
            val order = uiState.order
            if (order != null) {
                val deliveryLatLng = LatLng(order.deliveryLatitude, order.deliveryLongitude)
                val bounds = LatLngBounds.builder()
                    .include(courierLatLng)
                    .include(deliveryLatLng)
                    .build()

                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 120),
                    durationMs = 1000
                )
            }
        }
    }

    val accentGreen = Color(0xFF00C853)
    val accentOrange = Color(0xFFFF6D00)
    val darkBg = Color(0xFF1A1A2E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Buyurtma #${uiState.order?.orderNumber ?: "..."}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = uiState.orderStatus.displayName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading holati
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentGreen)
                }
                return@Scaffold
            }

            // ==================== XARITA ====================
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                val order = uiState.order

                // 📍 Yetkazish manzili markeri
                if (order != null) {
                    Marker(
                        state = MarkerState(
                            position = LatLng(order.deliveryLatitude, order.deliveryLongitude)
                        ),
                        title = "Yetkazish manzili",
                        snippet = order.deliveryAddress,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }

                // 🚴 Kuryer markeri (animatsiyalangan)
                if (animatedLat != 0f && animatedLng != 0f) {
                    Marker(
                        state = MarkerState(
                            position = LatLng(animatedLat.toDouble(), animatedLng.toDouble())
                        ),
                        title = "Kuryer",
                        rotation = animatedBearing,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_ORANGE
                        ),
                        flat = true, // Xarita bilan birga aylanadi
                        anchor = java.drawing.PointF(0.5f, 0.5f) // Markazlangan
                    )

                    // Kuryer yo'li (polyline)
                    if (order != null) {
                        Polyline(
                            points = listOf(
                                LatLng(animatedLat.toDouble(), animatedLng.toDouble()),
                                LatLng(order.deliveryLatitude, order.deliveryLongitude)
                            ),
                            color = accentOrange,
                            width = 8f
                        )
                    }
                }
            }

            // ==================== PASTKI PANEL ====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Status Timeline Card
                AnimatedVisibility(
                    visible = !uiState.isLoading,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // Buyurtma holati sarlavhasi
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Pulsating indicator
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseAlpha"
                                )

                                val statusColor = when (uiState.orderStatus) {
                                    OrderStatus.DELIVERED -> accentGreen
                                    OrderStatus.CANCELLED -> Color.Red
                                    OrderStatus.DELIVERING -> accentOrange
                                    else -> Color(0xFF448AFF)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(statusColor.copy(alpha = pulseAlpha))
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = uiState.orderStatus.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = statusColor
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                uiState.order?.let { order ->
                                    Text(
                                        text = "${order.totalAmount.toInt()} so'm",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Status timeline
                            OrderStatusTimeline(
                                currentStatus = uiState.orderStatus,
                                accentGreen = accentGreen
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Yetkazish ma'lumotlari
                            uiState.order?.let { order ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = order.deliveryAddress,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Buyurtma holatlari timeline komponenti
 */
@Composable
private fun OrderStatusTimeline(
    currentStatus: OrderStatus,
    accentGreen: Color
) {
    val steps = listOf(
        StatusStep(Icons.Default.CheckCircle, "Tasdiqlandi", OrderStatus.CONFIRMED),
        StatusStep(Icons.Default.Restaurant, "Tayyorlanmoqda", OrderStatus.PREPARING),
        StatusStep(Icons.Default.DeliveryDining, "Yetkazilmoqda", OrderStatus.DELIVERING),
        StatusStep(Icons.Default.LocationOn, "Yetkazildi", OrderStatus.DELIVERED),
    )

    val currentIndex = steps.indexOfFirst { it.status == currentStatus }
        .coerceAtLeast(0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isCompleted = index <= currentIndex
            val isCurrent = index == currentIndex

            val iconColor by animateColorAsState(
                targetValue = when {
                    isCompleted -> accentGreen
                    else -> Color.Gray.copy(alpha = 0.3f)
                },
                animationSpec = tween(500),
                label = "iconColor"
            )

            val iconSize by animateFloatAsState(
                targetValue = if (isCurrent) 28f else 22f,
                animationSpec = tween(300),
                label = "iconSize"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = step.label,
                    tint = iconColor,
                    modifier = Modifier.size(iconSize.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.label,
                    fontSize = 10.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color.Gray.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}

private data class StatusStep(
    val icon: ImageVector,
    val label: String,
    val status: OrderStatus
)
