package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Geometric Balance Design Palette
val GeoBackground = Color(0xFFF7F9FF)
val GeoSurface = Color(0xFFFFFFFF)
val GeoSurfaceVariant = Color(0xFFF0F4F8)
val GeoBorder = Color(0xFFE1E2E9)
val GeoBorderLight = Color(0xFFF0F1F7)
val GeoOutline = Color(0xFFC2C7CF)

val GeoTextPrimary = Color(0xFF001D36)
val GeoTextBody = Color(0xFF191C1E)
val GeoTextSecondary = Color(0xFF44474E)

// Primary Brand Blue
val GeoBluePrimary = Color(0xFF0061A4)
val GeoBlueContainer = Color(0xFFD1E4FF)
val GeoOnBlueContainer = Color(0xFF001D36)

// Secondary Purple Container (for Projects stat card)
val GeoPurplePrimary = Color(0xFF6750A4)
val GeoPurpleContainer = Color(0xFFEADDFF)
val GeoOnPurpleContainer = Color(0xFF21005D)

// Success / Present Mint Green Container
val GeoGreenPrimary = Color(0xFF006D38)
val GeoGreenContainer = Color(0xFFC4EED0)
val GeoOnGreenContainer = Color(0xFF00210B)

// Error / Absent Pastel Coral/Red Container
val GeoRedPrimary = Color(0xFFBA1A1A)
val GeoRedContainer = Color(0xFFFFDAD6)
val GeoOnRedContainer = Color(0xFF410002)

// Warning / Half Day Warm Amber Container
val GeoAmberPrimary = Color(0xFF745B00)
val GeoAmberContainer = Color(0xFFFFE088)
val GeoOnAmberContainer = Color(0xFF261A00)

// Backward compatible semantic aliases for the app
val BluePrimary = GeoBluePrimary
val BluePrimaryContainer = GeoBlueContainer
val OnBluePrimaryContainer = GeoOnBlueContainer

val SlateSecondary = GeoTextSecondary
val SlateSecondaryContainer = GeoBorder
val OnSlateSecondaryContainer = GeoTextPrimary

val AmberTertiary = GeoAmberPrimary
val AmberTertiaryContainer = GeoAmberContainer

val AppBackground = GeoBackground
val AppSurface = GeoSurface
val AppSurfaceVariant = GeoSurfaceVariant
val AppOutline = GeoOutline
val AppOutlineVariant = GeoBorder

val StatusPresent = GeoOnGreenContainer
val StatusPresentBg = GeoGreenContainer
val StatusHalfDay = GeoOnAmberContainer
val StatusHalfDayBg = GeoAmberContainer
val StatusAbsent = GeoOnRedContainer
val StatusAbsentBg = GeoRedContainer
val StatusPending = GeoBluePrimary
val StatusPendingBg = GeoBlueContainer
