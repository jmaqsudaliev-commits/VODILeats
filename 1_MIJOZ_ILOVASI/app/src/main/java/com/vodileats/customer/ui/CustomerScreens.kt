@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vodileats.customer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vodileats.customer.data.*
import com.vodileats.customer.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ==================== VIEWMODELS ====================

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val api: CustomerApi,
    private val cartDao: CartDao
) : ViewModel() {

    var restaurants by mutableStateOf<List<Restaurant>>(emptyList())
    var filteredRestaurants by mutableStateOf<List<Restaurant>>(emptyList())
    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf("Barchasi")
    var isLoading by mutableStateOf(false)

    var currentRestaurant by mutableStateOf<Restaurant?>(null)
    var categories by mutableStateOf<List<Category>>(emptyList())

    val cartItems = cartDao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cartCount = cartDao.getCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadRestaurants()
    }

    fun loadRestaurants() {
        viewModelScope.launch {
            isLoading = true
            try {
                val res = api.getRestaurants()
                if (res.isSuccessful && res.body() != null) {
                    restaurants = res.body()!!.data
                    filteredRestaurants = restaurants
                }
            } catch (e: Exception) {
                // error handled
            } finally {
                isLoading = false
            }
        }
    }

    fun search(q: String) {
        searchQuery = q
        filteredRestaurants = if (q.isBlank()) restaurants else {
            restaurants.filter { it.name.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) }
        }
    }

    fun loadRestaurantDetails(id: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val restRes = api.getRestaurant(id)
                val menuRes = api.getMenu(id)
                if (restRes.isSuccessful) currentRestaurant = restRes.body()
                if (menuRes.isSuccessful) categories = menuRes.body() ?: emptyList()
            } catch (e: Exception) {}
            isLoading = false
        }
    }

    fun addToCart(item: MenuItem, restaurant: Restaurant) {
        viewModelScope.launch {
            val existing = cartItems.value.firstOrNull { it.menuItemId == item.id }
            if (existing != null) {
                cartDao.updateQuantity(item.id, existing.quantity + 1)
            } else {
                cartDao.insertOrUpdate(
                    CartItem(
                        menuItemId = item.id,
                        restaurantId = restaurant.id,
                        restaurantName = restaurant.name,
                        name = item.name,
                        price = item.price,
                        quantity = 1,
                        imageUrl = item.imageUrl
                    )
                )
            }
        }
    }

    fun removeFromCart(menuItemId: String) {
        viewModelScope.launch {
            val existing = cartItems.value.firstOrNull { it.menuItemId == menuItemId } ?: return@launch
            if (existing.quantity > 1) {
                cartDao.updateQuantity(menuItemId, existing.quantity - 1)
            } else {
                cartDao.deleteById(menuItemId)
            }
        }
    }

    fun incrementCartItem(menuItemId: String) {
        viewModelScope.launch {
            val existing = cartItems.value.firstOrNull { it.menuItemId == menuItemId } ?: return@launch
            cartDao.updateQuantity(menuItemId, existing.quantity + 1)
        }
    }

    fun decrementCartItem(menuItemId: String) {
        viewModelScope.launch {
            val existing = cartItems.value.firstOrNull { it.menuItemId == menuItemId } ?: return@launch
            if (existing.quantity > 1) {
                cartDao.updateQuantity(menuItemId, existing.quantity - 1)
            } else {
                cartDao.deleteById(menuItemId)
            }
        }
    }

    fun removeCartItem(menuItemId: String) {
        viewModelScope.launch {
            cartDao.deleteById(menuItemId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartDao.clear()
        }
    }

    fun checkout(address: String, note: String, payment: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val items = cartItems.value
            if (items.isEmpty()) return@launch
            try {
                val body = mapOf(
                    "restaurantId" to items.first().restaurantId,
                    "deliveryAddress" to address,
                    "deliveryLatitude" to 40.1772,
                    "deliveryLongitude" to 71.7228,
                    "paymentMethod" to payment,
                    "customerNote" to note,
                    "items" to items.map { mapOf("menuItemId" to it.menuItemId, "name" to it.name, "price" to it.price, "quantity" to it.quantity) }
                )
                val res = api.createOrder(body)
                if (res.isSuccessful && res.body() != null) {
                    cartDao.clear()
                    onComplete(res.body()!!.id)
                }
            } catch (e: Exception) {}
        }
    }
}

// ==================== SCREENS ====================

@Composable
fun CatalogScreen(
    viewModel: CustomerViewModel = hiltViewModel(),
    onRestaurantClick: (String) -> Unit,
    onCartClick: () -> Unit
) {
    val cartCount by viewModel.cartCount.collectAsState()
    val categoriesList = listOf("Barchasi", "Milliy taomlar", "Fast Food", "Somsa", "Shashlik", "Ichimliklar")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Yetkazish manzili", fontSize = 11.sp, color = Gray500)
                            Text("Vodil markazi, Navoiy 12", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Box {
                        IconButton(
                            onClick = onCartClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(OrangeContainer)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = OrangePrimary)
                        }
                        if (cartCount > 0) {
                            Badge(containerColor = OrangePrimary, modifier = Modifier.align(Alignment.TopEnd)) {
                                Text(cartCount.toString(), color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.search(it) },
                    placeholder = { Text("Restoran yoki taom qidirish...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Gray500) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoriesList) { cat ->
                        val isSelected = cat == viewModel.selectedCategory
                        Surface(
                            onClick = { viewModel.selectedCategory = cat },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 4.dp else 1.dp
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else Gray700,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (viewModel.filteredRestaurants.isEmpty() && !viewModel.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = OrangeContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Hozircha restoranlar mavjud emas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Admin panel orqali yangi restoranlar qo'shilgach, ular bu yerda paydo bo'ladi.",
                            fontSize = 13.sp,
                            color = Gray500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadRestaurants() },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Qayta tekshirish")
                        }
                    }
                }
            } else {
                items(viewModel.filteredRestaurants) { restaurant ->
                    Card(
                        onClick = { onRestaurantClick(restaurant.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            AsyncImage(
                                model = restaurant.imageUrl ?: "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(restaurant.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(String.format("%.1f", restaurant.rating), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(restaurant.description, color = Gray500, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("⏱ ~${restaurant.avgDeliveryTimeMinutes} daqiqa", fontSize = 13.sp, color = Gray700)
                                    Text("•", color = Gray500)
                                    Text("Yetkazish: ${restaurant.deliveryFeeBase.toInt()} so'm", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantDetailScreen(
    restaurantId: String,
    viewModel: CustomerViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    LaunchedEffect(restaurantId) {
        viewModel.loadRestaurantDetails(restaurantId)
    }

    val cartItems by viewModel.cartItems.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState()
    val totalAmount = cartItems.sumOf { it.price * it.quantity }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = cartCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onNavigateToCart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Text("Savatchaga o'tish • ${totalAmount.toInt()} so'm ($cartCount)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    AsyncImage(
                        model = viewModel.currentRestaurant?.imageUrl ?: "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                }
            }

            item {
                viewModel.currentRestaurant?.let { r ->
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(r.name, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(r.address, color = Gray500, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Yetkazish: ~${r.avgDeliveryTimeMinutes} daqiqa • ${r.deliveryFeeBase.toInt()} so'm", color = Gray700, fontWeight = FontWeight.Medium)
                        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = Gray200)
                    }
                }
            }

            viewModel.categories.forEach { category ->
                item {
                    Text(category.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }

                items(category.items) { item ->
                    val inCart = cartItems.firstOrNull { it.menuItemId == item.id }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.imageUrl ?: "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=300",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("${item.price.toInt()} so'm", fontWeight = FontWeight.Bold, color = OrangePrimary, modifier = Modifier.padding(top = 4.dp))
                            }

                            if (inCart == null || inCart.quantity == 0) {
                                IconButton(
                                    onClick = { viewModel.currentRestaurant?.let { viewModel.addToCart(item, it) } },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(OrangePrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(OrangeContainer)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    IconButton(onClick = { viewModel.removeFromCart(item.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = OrangePrimary)
                                    }
                                    Text(inCart.quantity.toString(), fontWeight = FontWeight.Bold, color = OnOrangeContainer, modifier = Modifier.padding(horizontal = 4.dp))
                                    IconButton(onClick = { viewModel.currentRestaurant?.let { viewModel.addToCart(item, it) } }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = OrangePrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(70.dp)) }
        }
    }
}

@Composable
fun CartScreen(
    viewModel: CustomerViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit
) {
    val items by viewModel.cartItems.collectAsState()
    var address by remember { mutableStateOf("Vodil markazi, Mustaqillik ko'chasi 14") }
    var note by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("CASH") }

    val subtotal = items.sumOf { it.price * it.quantity }
    val deliveryFee = if (items.isNotEmpty()) 12000.0 else 0.0
    val total = subtotal + deliveryFee

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savatcha", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Tozalash", tint = Gray500)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
                    Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taomlar:", fontSize = 13.sp, color = Gray700)
                            Text("${subtotal.toInt()} so'm", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Yetkazib berish:", fontSize = 13.sp, color = Gray700)
                            Text("${deliveryFee.toInt()} so'm", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Gray200)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Jami:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray700)
                            Text("${total.toInt()} so'm", fontWeight = FontWeight.Black, fontSize = 20.sp, color = OrangePrimary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.checkout(address, note, payment, onOrderPlaced) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Text("Buyurtma berish (${total.toInt()} so'm)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = OrangeContainer,
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("Savatchangiz bo'sh", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Restoranlar menyusidan mazali taomlarni tanlang va savatchaga qo'shing",
                        fontSize = 14.sp,
                        color = Gray500,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Taomlarni tanlash", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.menuItemId }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.imageUrl != null && item.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${item.price.toInt()} so'm / dona", color = Gray500, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                Text("${(item.price * item.quantity).toInt()} so'm", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                            }

                            // - [quantity] + tugmalari
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(OrangeContainer)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.decrementCartItem(item.menuItemId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Kamaytirish (-)",
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = item.quantity.toString(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = OnOrangeContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                IconButton(
                                    onClick = { viewModel.incrementCartItem(item.menuItemId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Ko'paytirish (+)",
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Yetkazish manzili", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangePrimary) },
                        placeholder = { Text("Aniq manzilni kiriting") }
                    )
                }

                item {
                    Text("Izoh (ixtiyoriy)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Masalan: dom kodi, qo'ng'iroq qilmang...") }
                    )
                }

                item {
                    Text("To'lov turi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = payment == "CASH", onClick = { payment = "CASH" }, label = { Text("💵 Naqd pul") })
                        FilterChip(selected = payment == "PAYME", onClick = { payment = "PAYME" }, label = { Text("💳 Payme") })
                        FilterChip(selected = payment == "CLICK", onClick = { payment = "CLICK" }, label = { Text("📱 Click") })
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun TrackingScreen(
    orderId: String,
    onBack: () -> Unit
) {
    val courierPos = LatLng(40.1772, 71.7228)
    val customerPos = LatLng(40.1830, 71.7290)
    val cameraState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(courierPos, 15f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buyurtma Kuzatuvi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraState) {
                Marker(state = MarkerState(position = courierPos), title = "Kuryer yo'lda")
                Marker(state = MarkerState(position = customerPos), title = "Sizning manzilingiz")
                Polyline(points = listOf(courierPos, customerPos), color = OrangePrimary, width = 12f)
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                    Text("Kuryer buyurtmangizni olib kelmoqda 🚴", fontWeight = FontWeight.Black, fontSize = 18.sp, color = OrangePrimary)
                    Text("Taxminiy yetib borish vaqti: ~12 daqiqa", color = Gray700, modifier = Modifier.padding(top = 4.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Kuryer: Bobur Ergashev (+998 90 222 33 44)", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CustomerAuthScreen(
    api: CustomerApi,
    onAuthSuccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Parol bilan, 1: SMS orqali
    var phone by remember { mutableStateOf("+998903334455") }
    var password by remember { mutableStateOf("123456") }
    var otp by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var otpHintMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showServerDialog by remember { mutableStateOf(false) }
    var serverHostInput by remember { mutableStateOf(CustomerPrefs.getServerHost(context)) }
    val coroutineScope = rememberCoroutineScope()

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Server IP Sozlamasi", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ilova ulanadigan server manzilini tanlang:", fontSize = 13.sp, color = Gray700)
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
                    CustomerPrefs.setServerHost(context, serverHostInput)
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
                .padding(horizontal = 24.dp),
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
                Icon(Icons.Default.Restaurant, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("VODIL EATS", fontSize = 28.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
            Text("Mijoz Ilovasi", fontSize = 14.sp, color = Gray500, modifier = Modifier.padding(bottom = 16.dp))

            // 1-Click Quick Demo Login Button
            Surface(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val res = api.loginPassword(mapOf("phone" to "+998903334455", "password" to "123456"))
                            if (res.isSuccessful && res.body() != null) {
                                CustomerPrefs.saveUser(context, "customer_token", "+998903334455", "Azizbek")
                                onAuthSuccess()
                            } else {
                                // Serverga ulanib bo'lmagan bo'lsa ham offline rejimda ruxsat berish
                                CustomerPrefs.saveUser(context, "demo_token", "+998903334455", "Azizbek (Sinov)")
                                onAuthSuccess()
                            }
                        } catch (e: Exception) {
                            // Agar server hali o'chiq bo'lsa darhol sinovga o'tish
                            CustomerPrefs.saveUser(context, "demo_token", "+998903334455", "Azizbek (Sinov)")
                            onAuthSuccess()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = OrangeContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = OrangePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("⚡ 1-bosishda Tezkor Sinov (Demo Mijoz)", fontWeight = FontWeight.Bold, color = OnOrangeContainer, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs: Parol bilan kirish / SMS orqali ro'yxatdan o'tish
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; errorMessage = null },
                    text = { Text("Parol bilan kirish", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; errorMessage = null },
                    text = { Text("SMS orqali / Ro'yxat", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Login with Phone + Password
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
                                val res = api.loginPassword(mapOf("phone" to phone.trim(), "password" to password))
                                if (res.isSuccessful && res.body() != null) {
                                    CustomerPrefs.saveUser(context, "token", phone.trim(), "Mijoz")
                                    onAuthSuccess()
                                } else {
                                    errorMessage = "Telefon raqam yoki parol noto'g'ri. Iltimos tekshiring yoki SMS orqali kiring."
                                }
                            } catch (e: Exception) {
                                errorMessage = "Tarmoq xatosi: Serverga (${CustomerPrefs.getServerHost(context)}) ulanib bo'lmadi. Sozlamalardan IP ni tekshiring."
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
                // SMS OTP Registration / Login with Password Creation
                if (!isOtpSent) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefon raqam") },
                        placeholder = { Text("+998 90 123 45 67") },
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
                                    val res = api.sendOtp(mapOf("phone" to phone.trim()))
                                    if (res.isSuccessful) {
                                        isOtpSent = true
                                        val body = res.body()
                                        val serverOtp = body?.get("otp")
                                        if (!serverOtp.isNullOrBlank()) {
                                            otp = serverOtp
                                            otpHintMessage = "SMS kod olindi: $serverOtp (Avtomatik to'ldirildi)"
                                        } else {
                                            otpHintMessage = "SMS kod yuborildi. Kodni kiriting."
                                        }
                                    } else {
                                        errorMessage = "SMS yuborishda xatolik. Telefon raqamini to'g'ri kiriting."
                                    }
                                } catch (e: Exception) {
                                    // Avto-fallback for test
                                    isOtpSent = true
                                    otp = "123456"
                                    otpHintMessage = "Test rejimi: Kod 123456 to'ldirildi"
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
                        else Text("SMS kodni olish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    if (otpHintMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FreshGreen.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "✅ " + otpHintMessage!!,
                                color = FreshGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("SMS Tasdiqlash Kodi") },
                        placeholder = { Text("123456") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Ismingiz") },
                        placeholder = { Text("Azizbek") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Parol yarating") },
                        placeholder = { Text("123456") },
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
                                    val body = mutableMapOf(
                                        "phone" to phone.trim(),
                                        "otp" to otp.trim(),
                                        "firstName" to (if (firstName.isBlank()) "Mijoz" else firstName.trim()),
                                        "password" to (if (newPassword.isBlank()) "123456" else newPassword.trim()),
                                        "role" to "customer"
                                    )
                                    val res = api.verifyOtp(body)
                                    if (res.isSuccessful && res.body() != null) {
                                        CustomerPrefs.saveUser(context, "token", phone.trim(), firstName.ifBlank { "Mijoz" })
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = "SMS kod yoki ma'lumotlar xato"
                                    }
                                } catch (e: Exception) {
                                    // Offline fallback
                                    CustomerPrefs.saveUser(context, "token", phone.trim(), firstName.ifBlank { "Mijoz" })
                                    onAuthSuccess()
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
                        else Text("Ro'yxatdan o'tish va Kirish", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
