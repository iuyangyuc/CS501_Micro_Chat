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
    primary = PrimaryBlueBright,
    onPrimary = Color(0xFF00315F),
    primaryContainer = PrimaryBlueContainerDark,
    onPrimaryContainer = PrimaryBlueContainerLight,
    secondary = SecondaryBlueBright,
    onSecondary = Color(0xFF10305E),
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = SecondaryContainerLight,
    tertiary = BrightGold,
    onTertiary = DarkGold,
    tertiaryContainer = Color(0xFF513D00),
    onTertiaryContainer = BrightGold,
    background = SurfaceBackgroundDark,
    onBackground = SurfaceOnDark,
    surface = SurfaceBackgroundDark,
    onSurface = SurfaceOnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OutlineVariantLight,
    surfaceTint = PrimaryBlueBright,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = SurfaceDimDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseOnSurface = SurfaceOnLight,
    inverseSurface = SurfaceBackgroundLight,
    inversePrimary = PrimaryBlue,
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueContainerLight,
    onPrimaryContainer = NavyBlue,
    secondary = DeepBlue,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = NavyBlue,
    tertiary = WarmGold,
    onTertiary = DarkGold,
    tertiaryContainer = BrightGold,
    onTertiaryContainer = DarkGold,
    background = SurfaceBackgroundLight,
    onBackground = SurfaceOnLight,
    surface = SurfaceBackgroundLight,
    onSurface = SurfaceOnLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OutlineLight,
    surfaceTint = PrimaryBlue,
    surfaceBright = SurfaceBrightLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseOnSurface = SurfaceOnDark,
    inverseSurface = SurfaceBackgroundDark,
    inversePrimary = PrimaryBlueBright,
    scrim = Color(0xFF000000)
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
