package com.vodileats.feature.restaurant.orders

import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vodileats.core.domain.model.Order
import com.vodileats.core.domain.model.OrderStatus

/**
 * RESTORAN BUYURTMALAR BOSHQARUV EKRANI
 *
 * Xususiyatlar:
 * - Yangi buyurtma tushganda baland ovozli signal
 * - Buyurtmani qabul qilish / rad etish
 * - Status o'tkazish (Tayyorlash -> Tayyor)
 * - Real-time WebSocket orqali yangilanish
 */
@Composable
fun RestaurantOrdersScreen(
    viewModel: RestaurantOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Yangi buyurtma kelganda ovoz chiqarish
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(uiState.newOrderAlert) {
        if (uiState.newOrderAlert) {
            try {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(context, alarmUri)?.apply {
                    isLooping = true
                    start()
                }
            } catch (e: Exception) {
                // Fallback: system notification sound
            }
        } else {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    val accentGreen = Color(0xFF00C853)
    val accentOrange = Color(0xFFFF6D00)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = if (uiState.newOrderAlert) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF6D00), Color(0xFFFF9100))
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                            )
                        }
                    )
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Buyurtmalar",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${uiState.activeOrders.size} ta faol buyurtma",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Yangi buyurtma alert
                AnimatedVisibility(
                    visible = uiState.newOrderAlert,
                    enter = scaleIn() + fadeIn()
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "bell")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(300),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bellScale"
                    )

                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Yangi buyurtma!",
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .scale(scale)
                    )
                }
            }

            // Buyurtmalar ro'yxati
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.activeOrders, key = { it.id }) { order ->
                    RestaurantOrderCard(
                        order = order,
                        onConfirm = { viewModel.confirmOrder(order.id) },
                        onStartPreparing = { viewModel.startPreparing(order.id) },
                        onMarkReady = { viewModel.markReady(order.id) },
                        onCancel = { viewModel.cancelOrder(order.id) },
                        onDismissNewAlert = { viewModel.dismissNewOrderAlert() },
                        isNew = uiState.newOrderAlert && order.status == OrderStatus.PENDING,
                        accentGreen = accentGreen,
                        accentOrange = accentOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun RestaurantOrderCard(
    order: Order,
    onConfirm: () -> Unit,
    onStartPreparing: () -> Unit,
    onMarkReady: () -> Unit,
    onCancel: () -> Unit,
    onDismissNewAlert: () -> Unit,
    isNew: Boolean,
    accentGreen: Color,
    accentOrange: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNew) {
                accentOrange.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNew) 8.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Buyurtma sarlavhasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNew) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentOrange)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "#${order.orderNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = order.status.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (order.status) {
                        OrderStatus.PENDING -> accentOrange
                        OrderStatus.PREPARING -> Color(0xFF448AFF)
                        OrderStatus.READY_FOR_PICKUP -> accentGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buyurtma tarkibi
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}x ${item.name}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.totalPrice.toInt()} so'm",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Jami summa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Jami:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${order.totalAmount.toInt()} so'm",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = accentGreen
                )
            }

            // Mijoz izohi
            order.customerNote?.let { note ->
                if (note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📝 $note",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Harakatlar tugmalari
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (order.status) {
                    OrderStatus.PENDING -> {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rad etish", fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                onConfirm()
                                onDismissNewAlert()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentGreen
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Qabul qilish", fontSize = 13.sp)
                        }
                    }

                    OrderStatus.CONFIRMED -> {
                        Button(
                            onClick = onStartPreparing,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF448AFF)
                            )
                        ) {
                            Icon(Icons.Default.Fastfood, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tayyorlashni boshlash")
                        }
                    }

                    OrderStatus.PREPARING -> {
                        Button(
                            onClick = onMarkReady,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentGreen
                            )
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kuryerga topshirishga tayyor")
                        }
                    }

                    else -> { /* Boshqa statuslarda tugma yo'q */ }
                }
            }
        }
    }
}
