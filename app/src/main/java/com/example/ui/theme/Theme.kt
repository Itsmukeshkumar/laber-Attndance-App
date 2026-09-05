package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeometricLightColorScheme = lightColorScheme(
    primary = GeoBluePrimary,
    onPrimary = Color.White,
    primaryContainer = GeoBlueContainer,
    onPrimaryContainer = GeoOnBlueContainer,
    secondary = GeoPurplePrimary,
    onSecondary = Color.White,
    secondaryContainer = GeoPurpleContainer,
    onSecondaryContainer = GeoOnPurpleContainer,
    tertiary = GeoAmberPrimary,
    onTertiary = Color.White,
    tertiaryContainer = GeoAmberContainer,
    onTertiaryContainer = GeoOnAmberContainer,
    background = GeoBackground,
    onBackground = GeoTextPrimary,
    surface = GeoSurface,
    onSurface = GeoTextBody,
    surfaceVariant = GeoSurfaceVariant,
    onSurfaceVariant = GeoTextSecondary,
    outline = GeoOutline,
    outlineVariant = GeoBorder,
    error = GeoRedPrimary,
    errorContainer = GeoRedContainer,
    onErrorContainer = GeoOnRedContainer
)

private val GeometricDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFD0BCFF),
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF4F378B),
    onSecondaryContainer = Color(0xFFEADDFF),
    tertiary = Color(0xFFE5C16C),
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE088),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE0E2E8),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE0E2E8),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent Geometric Balance palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) GeometricDarkColorScheme else GeometricLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
