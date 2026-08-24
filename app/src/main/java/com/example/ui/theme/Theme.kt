package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ThemeGold500 = Color(0xFFF59E0B)
private val ThemeGold400 = Color(0xFFFBBF24)
private val ThemeGold600 = Color(0xFFD97706)
private val ThemeEmerald500 = Color(0xFF10B981)
private val ThemeEmerald600 = Color(0xFF059669)
private val ThemeNavy900 = Color(0xFF0F172A)
private val ThemeNavy800 = Color(0xFF1E293B)
private val ThemeSlate50 = Color(0xFFF8FAFC)

private val DarkColorScheme = darkColorScheme(
    primary = ThemeGold500,
    secondary = ThemeGold400,
    tertiary = ThemeEmerald500,
    background = ThemeNavy900,
    surface = ThemeNavy800,
    onPrimary = ThemeNavy900,
    onSecondary = ThemeNavy900,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = ThemeNavy900,
    secondary = ThemeGold600,
    tertiary = ThemeEmerald600,
    background = ThemeSlate50,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = ThemeNavy900,
    onSurface = ThemeNavy900,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

@Composable
fun StoreManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, content = content)
}
