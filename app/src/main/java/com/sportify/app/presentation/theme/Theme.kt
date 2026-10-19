package com.sportify.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0B7A4B), onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F2CF), onPrimaryContainer = Color(0xFF002111),
    secondary = Color(0xFF4F6354), tertiary = Color(0xFFE8762C)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF6CDBA0), onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF005230), onPrimaryContainer = Color(0xFFB7F2CF),
    secondary = Color(0xFFB4CCB9), tertiary = Color(0xFFFFB68B)
)

@Composable
fun SportifyTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
