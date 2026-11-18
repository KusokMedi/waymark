package com.kusokmedi.waymark.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF335F52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7D9C9),
    onPrimaryContainer = Color(0xFF062018),
    secondary = Color(0xFF4E6359),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8DC),
    onSecondaryContainer = Color(0xFF0B1F16),
    tertiary = Color(0xFF6C5D2F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5E1A8),
    onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFFBF9F4),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFFBF9F4),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFDCE5DE),
    onSurfaceVariant = Color(0xFF404943),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD0BC),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF1B4D3E),
    onPrimaryContainer = Color(0xFFB7D9C9),
    secondary = Color(0xFFB5CCC0),
    onSecondary = Color(0xFF20342B),
    secondaryContainer = Color(0xFF364B41),
    onSecondaryContainer = Color(0xFFD1E8DC),
    tertiary = Color(0xFFD8C58D),
    onTertiary = Color(0xFF3B2F04),
    tertiaryContainer = Color(0xFF534619),
    onTertiaryContainer = Color(0xFFF5E1A8),
    background = Color(0xFF121614),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF121614),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun WaymarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
