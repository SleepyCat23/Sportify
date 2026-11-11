package com.sportify.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette taken from the Sportify web design
val Green = Color(0xFF0B7B4E)
val Mint = Color(0xFFB8F0D0)
val OnMint = Color(0xFF00391F)
val Ink = Color(0xFF1A1C19)
val Muted = Color(0xFF4F6355)
val Peach = Color(0xFFFFDCC4)
val PeachText = Color(0xFF5A2A00)
val Orange = Color(0xFFE8762C)
val TagBg = Color(0xFFE3EEE4)
val DisabledBg = Color(0xFFEAEBE8)
val Border = Color(0xFFDDE3DA)
val Canvas = Color(0xFFFAFBF7)

private val LightColors = lightColorScheme(
    primary = Green, onPrimary = Color.White,
    primaryContainer = Mint, onPrimaryContainer = OnMint,
    secondary = Muted, secondaryContainer = Mint, onSecondaryContainer = OnMint,
    tertiary = Orange, tertiaryContainer = Peach, onTertiaryContainer = PeachText,
    background = Canvas, onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = TagBg, onSurfaceVariant = Muted,
    surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Color(0xFF8C968C), outlineVariant = Border
)

/** The design is light-only, so the app always uses the light scheme. */
@Composable
fun SportifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
