package com.vodileats.restaurant.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OrangePrimary = Color(0xFFFF6B00)
val OrangeContainer = Color(0xFFFFEAD9)
val OnOrangeContainer = Color(0xFF4D1D00)
val FreshGreen = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val RedError = Color(0xFFEF4444)
val BlueInfo = Color(0xFF3B82F6)
val Gray200 = Color(0xFFE2E8F0)
val Gray500 = Color(0xFF64748B)
val Gray700 = Color(0xFF334155)
val CharcoalBackground = Color(0xFF0F172A)
val CharcoalSurface = Color(0xFF1E293B)

private val LightColors = lightColorScheme(
    primary = OrangePrimary,
    secondary = FreshGreen,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

private val DarkColors = darkColorScheme(
    primary = OrangePrimary,
    secondary = FreshGreen,
    background = CharcoalBackground,
    surface = CharcoalSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun RestaurantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
