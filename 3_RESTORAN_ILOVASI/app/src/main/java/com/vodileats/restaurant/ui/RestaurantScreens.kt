package com.vodileats.restaurant.ui

import android.media.RingtoneManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.vodileats.restaurant.theme.*

data class OrderItemModel(val name: String, val quantity: Int, val price: Double)

data class RestaurantOrderModel(
    val id: String,
    val orderNumber: String,
    var status: String, // CREATED, CONFIRMED, PREPARING, READY
    val totalAmount: Double,
    val customerName: String,
    val customerPhone: String,
    val address: String,
    val items: List<OrderItemModel>
)

data class MenuItemModel(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    var isAvailable: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantOrdersScreen() {
    val context = LocalContext.current

    var orders by remember {
        mutableStateOf(
            listOf(
                RestaurantOrderModel(
                    id = "1",
                    orderNumber = "VD-8891",
                    status = "CREATED",
                    totalAmount = 76000.0,
                    customerName = "Azizbek Rahimov",
                    customerPhone = "+998 90 333 44 55",
                    address = "Vodil markazi, 14-uy",
                    items = listOf(
                        OrderItemModel("To'y Oshi (Devzira)", 2, 38000.0),
                        OrderItemModel("Achchiq-chuchuk", 1, 8000.0)
                    )
                ),
                RestaurantOrderModel(
                    id = "2",
                    orderNumber = "VD-8892",
                    status = "PREPARING",
                    totalAmount = 54000.0,
                    customerName = "Mavluda Karimova",
                    customerPhone = "+998 91 123 45 67",
                    address = "Mustaqillik shoh ko'chasi 8",
                    items = listOf(
                        OrderItemModel("Vodil Tandir Somsa", 4, 9000.0),
                        OrderItemModel("Qo'y Go'shti Shashlik", 1, 18000.0)
                    )
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Yangi Buyurtmalar", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = RedError) {
                            Text(orders.count { it.status == "CREATED" }.toString(), color = Color.White)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Play sound alert for demo
                        try {
                            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            val r = RingtoneManager.getRingtone(context, notification)
                            r.play()
                        } catch (e: Exception) {}
                    }) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OrangePrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (order.status == "CREATED") OrangeContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${order.orderNumber}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = OrangePrimary)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (order.status) {
                                    "CREATED" -> RedError
                                    "CONFIRMED" -> OrangePrimary
                                    "PREPARING" -> AmberWarning
                                    "READY" -> FreshGreen
                                    else -> Color.Gray
                                }
                            ) {
                                Text(
                                    text = when (order.status) {
                                        "CREATED" -> "YANGI TUSHDI!"
                                        "CONFIRMED" -> "QABUL QILINDI"
                                        "PREPARING" -> "TAYYORLANMOQDA"
                                        "READY" -> "TAYYOR (KURYERDA)"
                                        else -> order.status
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text("Mijoz: ${order.customerName} (${order.customerPhone})", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Text("Manzil: ${order.address}", color = Color(0xFF64748B), fontSize = 13.sp)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Items
                        order.items.forEach { itm ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${itm.quantity}x ${itm.name}", fontWeight = FontWeight.Medium)
                                Text("${(itm.price * itm.quantity).toInt()} so'm", color = Color(0xFF475569))
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Jami:", fontWeight = FontWeight.Bold)
                            Text("${order.totalAmount.toInt()} so'm", fontWeight = FontWeight.Black, fontSize = 16.sp, color = OrangePrimary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status transition action button
                        when (order.status) {
                            "CREATED" -> {
                                Button(
                                    onClick = {
                                        orders = orders.map { if (it.id == order.id) it.copy(status = "CONFIRMED") else it }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreen)
                                ) {
                                    Text("Buyurtmani Qabul Qilish", fontWeight = FontWeight.Bold)
                                }
                            }
                            "CONFIRMED" -> {
                                Button(
                                    onClick = {
                                        orders = orders.map { if (it.id == order.id) it.copy(status = "PREPARING") else it }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                                ) {
                                    Text("Tayyorlashni Boshlash", fontWeight = FontWeight.Bold)
                                }
                            }
                            "PREPARING" -> {
                                Button(
                                    onClick = {
                                        orders = orders.map { if (it.id == order.id) it.copy(status = "READY") else it }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                ) {
                                    Text("Tayyor! (Kuryerni Chaqirish 🚀)", fontWeight = FontWeight.Bold)
                                }
                            }
                            "READY" -> {
                                Text("Kuryer buyurtmani olib ketishi kutilmoqda...", color = FreshGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantMenuScreen() {
    var menuItems by remember {
        mutableStateOf(
            listOf(
                MenuItemModel("1", "To'y Oshi (Devzira guruchda)", "Asosiy", 38000.0, true),
                MenuItemModel("2", "Vodil Tandir Somsa", "Somsa", 9000.0, true),
                MenuItemModel("3", "Qo'y Go'shtidan Shashlik", "Kabab", 18000.0, true),
                MenuItemModel("4", "Manti (Go'shtli)", "Asosiy", 7000.0, false), // already in stop-list
                MenuItemModel("5", "Achchiq-chuchuk", "Salatlar", 8000.0, true)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menyu va Stop-list", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(menuItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isAvailable) MaterialTheme.colorScheme.surface else Color(0xFFF1F5F9)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (item.isAvailable) Color.Black else Color.Gray)
                            Text("${item.price.toInt()} so'm", color = OrangePrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                            Text(
                                text = if (item.isAvailable) "Sotuvda mavjud" else "Stop-listda (Sotuvdan olingan)",
                                color = if (item.isAvailable) FreshGreen else RedError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Switch(
                            checked = item.isAvailable,
                            onCheckedChange = { checked ->
                                menuItems = menuItems.map { if (it.id == item.id) it.copy(isAvailable = checked) else it }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FreshGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = RedError.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }
}
