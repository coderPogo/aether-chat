package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val ObsidianBackground = Color(0xFF0A0B0E)
val CyberGray = Color(0xFF12141A)
val CyberCard = Color(0xFF1E2128)
val NeonGreen = Color(0xFF06B6D4) // Changed to high-contrast cyan-500
val NeonBlue = Color(0xFF22D3EE)  // Changed to cyan-400
val NeonPurple = Color(0xFF818CF8) // Changed to indigo-400
val TextPrimary = Color(0xFFE2E8F0)
val TextSecondary = Color(0xFF94A3B8)

val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = NeonGreen,
    secondary = NeonBlue,
    tertiary = NeonPurple,
    background = ObsidianBackground,
    surface = CyberCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)
