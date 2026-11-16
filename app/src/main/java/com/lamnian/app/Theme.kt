package com.lamnian.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF1877F2)

@Composable
fun LamnianTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF4599FF), onPrimary = Color.White,
        background = Color(0xFF18191A), surface = Color(0xFF242526), surfaceVariant = Color(0xFF3A3B3C),
        onBackground = Color(0xFFE4E6EB), onSurface = Color(0xFFE4E6EB), onSurfaceVariant = Color(0xFFB0B3B8)
    ) else lightColorScheme(
        primary = Blue, onPrimary = Color.White,
        background = Color(0xFFF0F2F5), surface = Color.White, surfaceVariant = Color(0xFFE4E6EB),
        onBackground = Color(0xFF1C1E21), onSurface = Color(0xFF1C1E21), onSurfaceVariant = Color(0xFF65676B)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
