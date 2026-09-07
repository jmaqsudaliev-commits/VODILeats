package com.vodileats.feature.courier.order

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vodileats.core.network.model.OrderOfferData
import kotlinx.coroutines.delay

/**
 * KURYER BUYURTMA TAKLIFI EKRANI
 *
 * Buyurtma taklifi kelganda ko'rsatiladigan full-screen dialog.
 * Xususiyatlar:
 * - 30 soniyalik countdown timer (doiraviy progress bilan)
 * - Restoran va yetkazish manzili ma'lumotlari
 * - Masofa va narx ko'rsatkichlari
 * - Qabul qilish / Rad etish tugmalari
 * - Vaqt tugaganda avtomatik rad etish
 */
@Composable
fun CourierOrderOfferScreen(
    offer: OrderOfferData,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    viewModel: CourierOrderOfferViewModel = hiltViewModel()
) {
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val totalTime = offer.timeoutSeconds.toFloat()

    val accentGreen = Color(0xFF00C853)
    val accentOrange = Color(0xFFFF6D00)

    // Countdown boshlash
    LaunchedEffect(offer) {
        viewModel.startCountdown(offer.timeoutSeconds)
    }

    // Vaqt tugaganda avtomatik rad etish
    LaunchedEffect(timeRemaining) {
        if (timeRemaining <= 0) {
            onReject()
        }
    }

    // Progress animatsiyasi
    val progress by animateFloatAsState(
        targetValue = timeRemaining / totalTime,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        ),
        label = "timerProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Countdown Timer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background doira
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )

                        // Progress doira
                        val sweepColor = when {
                            progress > 0.5f -> accentGreen
                            progress > 0.2f -> accentOrange
                            else -> Color.Red
                        }

                        drawArc(
                            color = sweepColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )
                    }

                    Text(
                        text = "${timeRemaining.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            timeRemaining > totalTime * 0.5f -> accentGreen
                            timeRemaining > totalTime * 0.2f -> accentOrange
                            else -> Color.Red
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Yangi buyurtma!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Restoran ma'lumotlari
                InfoRow(
                    icon = Icons.Default.Restaurant,
                    label = "Restoran",
                    value = offer.restaurantName,
                    subValue = offer.restaurantAddress,
                    iconTint = accentOrange
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Yetkazish manzili
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Yetkazish manzili",
                    value = offer.deliveryAddress,
                    iconTint = accentGreen
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Statistik ma'lumotlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox(
                        label = "Restoranga",
                        value = "${String.format("%.1f", offer.distanceToRestaurant)} km",
                        color = accentOrange
                    )
                    StatBox(
                        label = "Yetkazish",
                        value = "${String.format("%.1f", offer.estimatedDeliveryDistance)} km",
                        color = Color(0xFF448AFF)
                    )
                    StatBox(
                        label = "Summa",
                        value = "${offer.totalAmount.toInt()} so'm",
                        color = accentGreen
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tugmalar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rad etish")
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeliveryDining, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Qabul qilish")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subValue: String? = null,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            subValue?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
