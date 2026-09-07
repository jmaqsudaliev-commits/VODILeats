package com.vodileats.feature.restaurant.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vodileats.core.domain.model.MenuItem
import com.vodileats.core.ui.component.VodilTopBar
import com.vodileats.core.ui.theme.*

@Composable
fun MenuManagementScreen(
    viewModel: MenuManagementViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            VodilTopBar(
                title = "Menyu va Stop-list",
                onBackClick = onBackClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Taom nomi bo'yicha qidirish...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Gray500) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                state.categories.forEachIndexed { catIndex, category ->
                    val filteredItems = category.items.filter {
                        state.searchQuery.isBlank() || it.name.contains(state.searchQuery, ignoreCase = true)
                    }

                    if (filteredItems.isNotEmpty()) {
                        item {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }

                        itemsIndexed(category.items) { itemIndex, item ->
                            if (state.searchQuery.isBlank() || item.name.contains(state.searchQuery, ignoreCase = true)) {
                                MenuItemRow(
                                    item = item,
                                    onToggleAvailability = {
                                        viewModel.toggleAvailability(catIndex, itemIndex)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItemRow(
    item: MenuItem,
    onToggleAvailability: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isAvailable) MaterialTheme.colorScheme.surface else Gray100
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isAvailable) MaterialTheme.colorScheme.onSurface else Gray500
                )
                Text(
                    text = "${item.price.toInt()} so'm",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isAvailable) OrangePrimary else Gray400,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isAvailable) GreenContainer else ErrorContainer,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = if (item.isAvailable) "Mavjud" else "Stop-listda",
                        color = if (item.isAvailable) OnGreenContainer else RedError,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = item.isAvailable,
                    onCheckedChange = { onToggleAvailability() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FreshGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = RedError.copy(alpha = 0.6f)
                    )
                )
                Text(
                    text = if (item.isAvailable) "Faol" else "O'chiq",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray500
                )
            }
        }
    }
}
