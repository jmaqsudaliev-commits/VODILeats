@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import com.vodileats.restaurant.data.RestaurantApi
import com.vodileats.restaurant.data.RestaurantPrefs
import com.vodileats.restaurant.theme.*
import kotlinx.coroutines.launch

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

// ==================== 1. RESTAURANT ACCESS CODE LOGIN SCREEN ====================

@Composable
fun RestaurantAuthScreen(
    api: RestaurantApi,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var accessCode by remember { mutableStateOf("VDL-SHOH-101") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showServerDialog by remember { mutableStateOf(false) }
    var serverHostInput by remember { mutableStateOf(RestaurantPrefs.getServerHost(context)) }
    val coroutineScope = rememberCoroutineScope()

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Server IP Sozlamasi", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Server manzilini tanlang:", fontSize = 13.sp, color = Gray700)
                    OutlinedTextField(
                        value = serverHostInput,
                        onValueChange = { serverHostInput = it },
                        label = { Text("Server IP:Port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { serverHostInput = "192.168.1.20:3000" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Wi-Fi IP", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { serverHostInput = "10.0.2.2:3000" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Emulyator", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    RestaurantPrefs.setServerHost(context, serverHostInput)
                    showServerDialog = false
                }) {
                    Text("Saqlash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showServerDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Server sozlamasi", tint = Gray500)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(OrangeContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(42.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("VODIL EATS", fontSize = 28.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
            Text("Restoran Boshqaruvi", fontSize = 14.sp, color = Gray500, modifier = Modifier.padding(bottom = 20.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔑 Restoranga berilgan maxsus kod", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OrangePrimary)
                    Text(
                        "Ushbu kod Vodil Eats Super Admin panelidan beriladi. Kodni kiritib tasdiqlaganingizdan so'ng, oshxona buyurtmalarni qabul qilish va boshqarish huquqiga ega bo'ladi.",
                        fontSize = 12.sp,
                        color = Gray700,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Quick Test Chips
            Text("⚡ Tezkor Sinov Kodlari:", fontSize = 12.sp, color = Gray700, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { accessCode = "VDL-SHOH-101" },
                    shape = RoundedCornerShape(8.dp),
                    color = OrangeContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Shohona Osh\nVDL-SHOH-101", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnOrangeContainer, modifier = Modifier.padding(6.dp))
                }
                Surface(
                    onClick = { accessCode = "VDL-FAST-202" },
                    shape = RoundedCornerShape(8.dp),
                    color = OrangeContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Vodil Burger\nVDL-FAST-202", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnOrangeContainer, modifier = Modifier.padding(6.dp))
                }
            }

            OutlinedTextField(
                value = accessCode,
                onValueChange = { accessCode = it.uppercase() },
                label = { Text("Restoran Maxsus Kodi") },
                placeholder = { Text("VDL-SHOH-101") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (accessCode.isBlank()) {
                        errorMessage = "Iltimos, restoranning maxsus kodini kiriting!"
                        return@Button
                    }
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val res = api.loginByCode(mapOf("accessCode" to accessCode.trim().uppercase()))
                            if (res.isSuccessful && res.body() != null) {
                                val restObj = res.body()!!["restaurant"] as? Map<*, *>
                                val id = "${restObj?.get("id") ?: "1"}"
                                val name = "${restObj?.get("name") ?: "Restoran"}"
                                val isOpen = (restObj?.get("isOpen") as? Boolean) ?: true
                                RestaurantPrefs.saveRestaurant(context, id, name, accessCode.trim().uppercase(), isOpen)
                                onAuthSuccess()
                            } else {
                                errorMessage = "Maxsus kod noto'g'ri yoki restoran admin tomonidan faolsizlantirilgan"
                            }
                        } catch (e: Exception) {
                            // Offline fallback for testing
                            val name = if (accessCode.contains("FAST")) "Vodil Fast Food & Burger" else "Shohona Milliy Taomlar"
                            RestaurantPrefs.saveRestaurant(context, "demo_rest_id", name, accessCode.trim().uppercase(), true)
                            onAuthSuccess()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Faollashtirish va Ishni Boshlash 🚀", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ==================== 2. RESTAURANT ORDERS SCREEN ====================

@Composable
fun RestaurantOrdersScreen(
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val restaurantName = RestaurantPrefs.getRestaurantName(context)
    val accessCode = RestaurantPrefs.getAccessCode(context)
    var isOpen by remember { mutableStateOf(RestaurantPrefs.isOpen(context)) }

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
                    Column {
                        Text(restaurantName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Kod: $accessCode • ${if (isOpen) "Ochiq" else "Yopiq"}", fontSize = 11.sp, color = if (isOpen) FreshGreen else Color.Gray)
                    }
                },
                actions = {
                    Switch(
                        checked = isOpen,
                        onCheckedChange = {
                            isOpen = it
                            RestaurantPrefs.setOpen(context, it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = FreshGreen)
                    )
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Chiqish", tint = Gray700)
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
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Jonli Buyurtmalar", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Badge(containerColor = RedError) {
                        Text("${orders.count { it.status == "CREATED" }} ta yangi", color = Color.White)
                    }
                }
            }

            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.orderNumber, fontWeight = FontWeight.Black, fontSize = 18.sp, color = OrangePrimary)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (order.status) {
                                    "CREATED" -> RedError.copy(alpha = 0.1f)
                                    "CONFIRMED" -> BlueInfo.copy(alpha = 0.1f)
                                    "PREPARING" -> AmberWarning.copy(alpha = 0.1f)
                                    else -> FreshGreen.copy(alpha = 0.1f)
                                }
                            ) {
                                Text(
                                    text = when (order.status) {
                                        "CREATED" -> "YANGI"
                                        "CONFIRMED" -> "QABUL QILINDI"
                                        "PREPARING" -> "TAYYORLANMOQDA"
                                        else -> "TAYYOR (KURYERGA)"
                                    },
                                    color = when (order.status) {
                                        "CREATED" -> RedError
                                        "CONFIRMED" -> BlueInfo
                                        "PREPARING" -> AmberWarning
                                        else -> FreshGreen
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Mijoz: ${order.customerName} (${order.customerPhone})", fontSize = 13.sp, color = Gray700)
                        Text("Manzil: ${order.address}", fontSize = 13.sp, color = Gray500)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Gray200)

                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.quantity}x ${item.name}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${(item.price * item.quantity).toInt()} so'm", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Gray200)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Jami summa:", fontSize = 14.sp, color = Gray700)
                            Text("${order.totalAmount.toInt()} so'm", fontWeight = FontWeight.Black, fontSize = 18.sp, color = OrangePrimary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (order.status) {
                                "CREATED" -> {
                                    Button(
                                        onClick = {
                                            orders = orders.map { if (it.id == order.id) it.copy(status = "CONFIRMED") else it }
                                        },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BlueInfo)
                                    ) {
                                        Text("Qabul qilish", fontWeight = FontWeight.Bold)
                                    }
                                }
                                "CONFIRMED" -> {
                                    Button(
                                        onClick = {
                                            orders = orders.map { if (it.id == order.id) it.copy(status = "PREPARING") else it }
                                        },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                                    ) {
                                        Text("Oshxonaga berildi", fontWeight = FontWeight.Bold)
                                    }
                                }
                                "PREPARING" -> {
                                    Button(
                                        onClick = {
                                            orders = orders.map { if (it.id == order.id) it.copy(status = "READY") else it }
                                        },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FreshGreen)
                                    ) {
                                        Text("Tayyor (Kuryer kutilmoqda) 🚴", fontWeight = FontWeight.Bold)
                                    }
                                }
                                "READY" -> {
                                    Surface(
                                        color = FreshGreen.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Kuryer topshirib oldi ✅", color = FreshGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
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

// ==================== 3. RESTAURANT MENU STOP-LIST SCREEN ====================

@Composable
fun RestaurantMenuScreen(
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val restaurantName = RestaurantPrefs.getRestaurantName(context)

    var menuItems by remember {
        mutableStateOf(
            listOf(
                MenuItemModel("1", "To'y Oshi (Devzira)", "Asosiy", 38000.0, true),
                MenuItemModel("2", "Vodil Tandir Somsa", "Asosiy", 9000.0, true),
                MenuItemModel("3", "Qo'y Go'shti Shashlik", "Kabab", 18000.0, false), // Stop-list
                MenuItemModel("4", "Achchiq-chuchuk", "Salat", 8000.0, true),
                MenuItemModel("5", "Ko'k Choy", "Ichimlik", 5000.0, true)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$restaurantName • Menyu", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Chiqish")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Taomlar Stop-listi (Bor / Tugagan)", fontSize = 14.sp, color = Gray500)
            }

            items(menuItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${item.price.toInt()} so'm • ${item.category}", color = Gray500, fontSize = 13.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (item.isAvailable) "Mavjud" else "Stop-list",
                                color = if (item.isAvailable) FreshGreen else RedError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = item.isAvailable,
                                onCheckedChange = { isChecked ->
                                    menuItems = menuItems.map {
                                        if (it.id == item.id) it.copy(isAvailable = isChecked) else it
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
