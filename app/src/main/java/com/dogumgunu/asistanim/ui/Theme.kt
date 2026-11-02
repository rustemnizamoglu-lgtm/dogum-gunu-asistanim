package com.dogumgunu.asistanim.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFB8431F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = Color(0xFF8B5E34),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB8),
    onSecondaryContainer = Color(0xFF2C1600),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF6DED6),
    onSurfaceVariant = Color(0xFF53433F),
    outline = Color(0xFF85736E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59E),
    onPrimary = Color(0xFF601500),
    primaryContainer = Color(0xFF8A2A0C),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFF0BD8F),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3C14),
    onSecondaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFF1E1613),
    onBackground = Color(0xFFF1DFDA),
    surface = Color(0xFF1E1613),
    onSurface = Color(0xFFF1DFDA),
    surfaceVariant = Color(0xFF53433F),
    onSurfaceVariant = Color(0xFFD8C2BC),
    outline = Color(0xFFA08C87)
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
