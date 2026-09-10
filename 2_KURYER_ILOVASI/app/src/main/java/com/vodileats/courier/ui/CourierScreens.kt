@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vodileats.courier.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vodileats.courier.data.CourierApi
import com.vodileats.courier.data.CourierPrefs
import com.vodileats.courier.theme.*
import kotlinx.coroutines.launch

enum class CourierStep {
    TO_RESTAURANT,
    PICKUP_ORDER,
    TO_CUSTOMER,
    COMPLETED
}

// ==================== 1. COURIER AUTH SCREEN ====================

@Composable
fun CourierAuthScreen(
    api: CourierApi,
    onAuthSuccess: (isVerified: Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Kirish, 1: Ro'yxatdan o'tish
    var phone by remember { mutableStateOf("+998902223344") }
    var password by remember { mutableStateOf("123456") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("motorcycle") }
    var plateNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showServerDialog by remember { mutableStateOf(false) }
    var serverHostInput by remember { mutableStateOf(CourierPrefs.getServerHost(context)) }
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
                    CourierPrefs.setServerHost(context, serverHostInput)
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(OrangeContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("VODIL EATS", fontSize = 28.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
            Text("Kuryer Ilovasi", fontSize = 14.sp, color = Gray500, modifier = Modifier.padding(bottom = 16.dp))

            // Quick Demo Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val res = api.login(mapOf("phone" to "+998902223344", "password" to "123456"))
                                if (res.isSuccessful && res.body() != null) {
                                    val isVer = (res.body()?.get("isVerified") as? Boolean) ?: true
                                    CourierPrefs.saveCourier(context, "courier_token", "+998902223344", "Bobur Ergashev", isVer)
                                    onAuthSuccess(isVer)
                                } else {
                                    CourierPrefs.saveCourier(context, "demo_token", "+998902223344", "Bobur (Tasdiqlangan)", true)
                                    onAuthSuccess(true)
                                }
                            } catch (e: Exception) {
                                CourierPrefs.saveCourier(context, "demo_token", "+998902223344", "Bobur (Tasdiqlangan)", true)
                                onAuthSuccess(true)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = FreshGreen.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "⚡ Bobur (Tasdiqlangan)",
                        color = FreshGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Surface(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val res = api.login(mapOf("phone" to "+998907778899", "password" to "123456"))
                                if (res.isSuccessful && res.body() != null) {
                                    val isVer = (res.body()?.get("isVerified") as? Boolean) ?: false
                                    CourierPrefs.saveCourier(context, "pending_token", "+998907778899", "Javohir Toirov", isVer)
                                    onAuthSuccess(isVer)
                                } else {
                                    CourierPrefs.saveCourier(context, "pending_token", "+998907778899", "Javohir (Kutilmoqda)", false)
                                    onAuthSuccess(false)
                                }
                            } catch (e: Exception) {
                                CourierPrefs.saveCourier(context, "pending_token", "+998907778899", "Javohir (Kutilmoqda)", false)
                                onAuthSuccess(false)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = AmberWarning.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "⏳ Javohir (Kutilmoqda)",
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; errorMessage = null },
                    text = { Text("Kirish", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; errorMessage = null },
                    text = { Text("Ro'yxatdan o'tish", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Login Tab
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon raqam") },
                    placeholder = { Text("+998 90 123 45 67") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Parol") },
                    placeholder = { Text("Parolingizni kiriting") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val res = api.login(mapOf("phone" to phone.trim(), "password" to password))
                                if (res.isSuccessful && res.body() != null) {
                                    val body = res.body()!!
                                    val isVer = (body["isVerified"] as? Boolean) ?: false
                                    val userObj = body["user"] as? Map<*, *>
                                    val name = "${userObj?.get("firstName") ?: "Kuryer"}"
                                    CourierPrefs.saveCourier(context, "token", phone.trim(), name, isVer)
                                    onAuthSuccess(isVer)
                                } else {
                                    errorMessage = "Telefon raqami yoki parol noto'g'ri"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Tarmoq xatosi: Serverga (${CourierPrefs.getServerHost(context)}) ulanib bo'lmadi"
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
                    else Text("Kirish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                // Register Tab (Admin tasdig'i talab etiladi)
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Ismingiz *") },
                    placeholder = { Text("Alisher") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Familiyangiz") },
                    placeholder = { Text("Karimov") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon raqam *") },
                    placeholder = { Text("+998 90 123 45 67") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Parol yarating *") },
                    placeholder = { Text("123456") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Transport turi:", fontSize = 12.sp, color = Gray700, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("motorcycle" to "Mototsikl", "car" to "Mashina", "bicycle" to "Velosiped").forEach { (type, label) ->
                        FilterChip(
                            selected = vehicleType == type,
                            onClick = { vehicleType = type },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it },
                    label = { Text("Davlat raqami (masalan: 40A777AA)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (firstName.isBlank() || phone.isBlank() || password.isBlank()) {
                            errorMessage = "Iltimos, barcha majburiy maydonlarni to'ldiring!"
                            return@Button
                        }
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val body = mapOf(
                                    "firstName" to firstName.trim(),
                                    "lastName" to lastName.trim(),
                                    "phone" to phone.trim(),
                                    "password" to password.trim(),
                                    "vehicleType" to vehicleType,
                                    "vehiclePlateNumber" to plateNumber.trim()
                                )
                                val res = api.register(body)
                                if (res.isSuccessful && res.body() != null) {
                                    CourierPrefs.saveCourier(context, "token", phone.trim(), firstName.trim(), false)
                                    onAuthSuccess(false) // Admin tasdiqlashi kutilmoqda!
                                } else {
                                    errorMessage = "Ro'yxatdan o'tishda xatolik. Ushbu telefon raqam band bo'lishi mumkin."
                                }
                            } catch (e: Exception) {
                                // Offline fallback test rejimida
                                CourierPrefs.saveCourier(context, "token", phone.trim(), firstName.trim(), false)
                                onAuthSuccess(false)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreen)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Ro'yxatdan o'tish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Text(
                    "💡 Ro'yxatdan o'tgach, profilingiz Admin tomonidan tasdiqlanishi kerak.",
                    fontSize = 11.sp,
                    color = Gray500,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
            }
        }
    }
}

// ==================== 2. COURIER PENDING APPROVAL SCREEN ====================

@Composable
fun CourierPendingScreen(
    api: CourierApi,
    onApproved: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }
    var checkMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val courierPhone = CourierPrefs.getCourierPhone(context)
    val courierName = CourierPrefs.getCourierName(context)

    Scaffold { padding ->
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
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Arizangiz ko'rib chiqilmoqda!", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
            Text(
                "Hurmatli $courierName, kuryerlik arizangiz muvaffaqiyatli qabul qilindi. Administrator tomonidan profilingiz tasdiqlangandan so'ng sizga buyurtmalar qabul qilish imkoniyati ochiladi.",
                fontSize = 14.sp,
                color = Gray700,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Kuryer: $courierName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Telefon: $courierPhone", color = Gray700, fontSize = 13.sp)
                    Text("Holat: ⏳ Admin tasdig'i kutilmoqda", color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("💡 Admin paneldan kuryerni tasdiqlang, so'ng quyidagi tugmani bosing.", fontSize = 11.sp, color = Gray500, modifier = Modifier.padding(top = 6.dp))
                }
            }

            if (checkMessage != null) {
                Text(
                    text = checkMessage!!,
                    color = if (checkMessage!!.contains("tasdiqlandi")) FreshGreen else AmberWarning,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isChecking = true
                        checkMessage = null
                        try {
                            val res = api.checkStatus(courierPhone)
                            if (res.isSuccessful && res.body() != null) {
                                val isVer = (res.body()?.get("isVerified") as? Boolean) ?: false
                                if (isVer) {
                                    CourierPrefs.setVerified(context, true)
                                    checkMessage = "✅ Tabriklaymiz! Profilingiz tasdiqlandi!"
                                    onApproved()
                                } else {
                                    checkMessage = "⏳ Hali tasdiqlanmadi. Admin paneldan 'Tasdiqlash' tugmasini bosing."
                                }
                            } else {
                                checkMessage = "⏳ Hali tasdiqlanmadi. Iltimos, admin tasdiqlashini kuting."
                            }
                        } catch (e: Exception) {
                            checkMessage = "Tarmoq xatosi: Serverga ulanib bo'lmadi"
                        } finally {
                            isChecking = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isChecking,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isChecking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Statusni qayta tekshirish", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Chiqish (Boshqa hisobga o'tish)", color = Gray700)
            }
        }
    }
}

// ==================== 3. COURIER ACTIVE DASHBOARD SCREEN ====================

@Composable
fun CourierDashboardScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(true) }
    var currentStep by remember { mutableStateOf(CourierStep.TO_RESTAURANT) }
    val courierName = CourierPrefs.getCourierName(context)

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
                Marker(state = MarkerState(position = courierPos), title = "Siz ($courierName)")
                Marker(state = MarkerState(position = restaurantPos), title = "Shohona Milliy Taomlar")
                Marker(state = MarkerState(position = customerPos), title = "Mijoz: Vodil 14-uy")

                val target = if (currentStep == CourierStep.TO_RESTAURANT || currentStep == CourierStep.PICKUP_ORDER) {
                    restaurantPos
                } else {
                    customerPos
                }

                Polyline(points = listOf(courierPos, target), color = OrangePrimary, width = 12f)
            }

            // Top Status Bar: Courier Info + Online/Offline + Logout
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOnline) FreshGreen else Color(0xFF475569),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(courierName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text(if (isOnline) "ONLINE • Buyurtmalar ochiq" else "OFFLINE • Dam olishda", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            IconButton(onClick = onLogout, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Logout, contentDescription = "Chiqish", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
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
