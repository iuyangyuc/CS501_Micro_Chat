package com.example.cs501_micro_chat.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = Color(0xFFD5E3FF),
    secondary = Color(0xFFAEC6FF),
    onSecondary = Color(0xFF00285A),
    secondaryContainer = DeepBlue,
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = BrightGold,
    onTertiary = Color(0xFF241A00),
    tertiaryContainer = WarmGold,
    onTertiaryContainer = Color(0xFF201700),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF424753),
    onSurfaceVariant = Color(0xFFC2C6D0),
    outline = Color(0xFF8C9099),
    inverseOnSurface = Color(0xFF1B1B1F),
    inverseSurface = Color(0xFFE2E2E6),
    inversePrimary = PrimaryBlue,
    surfaceTint = Color(0xFFA8C7FF)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E3FF),
    onPrimaryContainer = Color(0xFF001B3C),
    secondary = DeepBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF001C3B),
    tertiary = WarmGold,
    onTertiary = Color(0xFF231A00),
    tertiaryContainer = BrightGold,
    onTertiaryContainer = Color(0xFF241A00),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE0E3EB),
    onSurfaceVariant = Color(0xFF434750),
    outline = Color(0xFF757982),
    inverseOnSurface = Color(0xFFF1F0F4),
    inverseSurface = Color(0xFF303033),
    inversePrimary = Color(0xFFA8C7FF),
    surfaceTint = PrimaryBlue
)

@Composable
fun CS501_Micro_ChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+. Disable by default to keep brand colors consistent.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
