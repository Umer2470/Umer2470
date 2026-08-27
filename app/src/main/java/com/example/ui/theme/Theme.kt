package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val ThemeGold500 = Color(0xFFF59E0B)
private val ThemeGold400 = Color(0xFFFBBF24)
private val ThemeGold600 = Color(0xFFD97706)
private val ThemeEmerald500 = Color(0xFF10B981)
private val ThemeEmerald600 = Color(0xFF059669)
private val ThemeNavy900 = Color(0xFF0F172A)
private val ThemeNavy800 = Color(0xFF1E293B)
private val ThemeSlate50 = Color(0xFFF8FAFC)

val AccentColorsLight = listOf(
    // 0: Royal Blue
    lightColorScheme(
        primary = Color(0xFF1D4ED8),
        secondary = Color(0xFF3B82F6),
        tertiary = ThemeGold600,
        background = ThemeSlate50,
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A),
    ),
    // 1: Emerald Green
    lightColorScheme(
        primary = Color(0xFF047857),
        secondary = Color(0xFF10B981),
        tertiary = ThemeGold600,
        background = Color(0xFFF0FDF4),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF064E3B),
        onSurface = Color(0xFF064E3B),
    ),
    // 2: Deep Purple
    lightColorScheme(
        primary = Color(0xFF6D28D9),
        secondary = Color(0xFF8B5CF6),
        tertiary = ThemeGold600,
        background = Color(0xFFFAF5FF),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF3B0764),
        onSurface = Color(0xFF3B0764),
    ),
    // 3: Warm Orange / Amber
    lightColorScheme(
        primary = Color(0xFFC2410C),
        secondary = Color(0xFFF97316),
        tertiary = Color(0xFFD97706),
        background = Color(0xFFFFFBEB),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF431407),
        onSurface = Color(0xFF431407),
    ),
    // 4: Crimson Red
    lightColorScheme(
        primary = Color(0xFFBE123C),
        secondary = Color(0xFFF43F5E),
        tertiary = ThemeGold600,
        background = Color(0xFFFFF1F2),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF881337),
        onSurface = Color(0xFF881337),
    ),
    // 5: Vibrant Teal
    lightColorScheme(
        primary = Color(0xFF0F766E),
        secondary = Color(0xFF14B8A6),
        tertiary = ThemeGold600,
        background = Color(0xFFF0FDFA),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF134E4A),
        onSurface = Color(0xFF134E4A),
    )
)

val AccentColorsDark = listOf(
    // 0: Royal Blue Dark
    darkColorScheme(
        primary = Color(0xFF60A5FA),
        secondary = Color(0xFF3B82F6),
        tertiary = ThemeGold400,
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        onPrimary = Color(0xFF0F172A),
        onSecondary = Color(0xFF0F172A),
        onTertiary = Color.White,
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC),
    ),
    // 1: Emerald Dark
    darkColorScheme(
        primary = Color(0xFF34D399),
        secondary = Color(0xFF10B981),
        tertiary = ThemeGold400,
        background = Color(0xFF064E3B),
        surface = Color(0xFF065F46),
        onPrimary = Color(0xFF022C22),
        onSecondary = Color(0xFF022C22),
        onTertiary = Color.White,
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC),
    ),
    // 2: Deep Purple Dark
    darkColorScheme(
        primary = Color(0xFFC084FC),
        secondary = Color(0xFF9333EA),
        tertiary = ThemeGold400,
        background = Color(0xFF2E1065),
        surface = Color(0xFF3B0764),
        onPrimary = Color(0xFF1E0740),
        onSecondary = Color(0xFF1E0740),
        onTertiary = Color.White,
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC),
    ),
    // 3: Warm Orange Dark
    darkColorScheme(
        primary = Color(0xFFFB923C),
        secondary = Color(0xFFEA580C),
        tertiary = ThemeGold400,
        background = Color(0xFF431407),
        surface = Color(0xFF7C2D12),
        onPrimary = Color(0xFF261208),
        onSecondary = Color(0xFF261208),
        onTertiary = Color.White,
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC),
    ),
    // 4: Crimson Red Dark
    darkColorScheme(
        primary = Color(0xFFFB7185),
        secondary = Color(0xFFE11D48),
        tertiary = ThemeGold400,
        background = Color(0xFF4C0519),
        surface = Color(0xFF881337),
        onPrimary = Color(0xFF3B0311),
        onSecondary = Color(0xFF3B0311),
        onTertiary = Color.White,
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC),
    ),
    // 5: Vibrant Teal Dark
    darkColorScheme(
        primary = Color(0xFF2DD4BF),
        secondary = Color(0xFF0D9488),
        tertiary = ThemeGold400,
        background = Color(0xFF134E4A),
        surface = Color(0xFF115E59),
        onPrimary = Color(0xFF042F2E),
        onSecondary = Color(0xFF042F2E),
        onTertiary = Color.White,
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC),
    )
)

val AccentColorsBlack = listOf(
    // 0: Royal Blue AMOLED Black
    darkColorScheme(
        primary = Color(0xFF60A5FA),
        secondary = Color(0xFF3B82F6),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF1E1E1E),
        onPrimary = Color(0xFF000000),
        onSecondary = Color(0xFF000000),
        onTertiary = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFE2E8F0)
    ),
    // 1: Emerald Green AMOLED Black
    darkColorScheme(
        primary = Color(0xFF34D399),
        secondary = Color(0xFF10B981),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF0E241B),
        onPrimary = Color(0xFF000000),
        onSecondary = Color(0xFF000000),
        onTertiary = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFE2E8F0)
    ),
    // 2: Deep Purple AMOLED Black
    darkColorScheme(
        primary = Color(0xFFC084FC),
        secondary = Color(0xFF9333EA),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF1F1035),
        onPrimary = Color(0xFF000000),
        onSecondary = Color(0xFF000000),
        onTertiary = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFE2E8F0)
    ),
    // 3: Warm Orange AMOLED Black
    darkColorScheme(
        primary = Color(0xFFFB923C),
        secondary = Color(0xFFF97316),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF261208),
        onPrimary = Color(0xFF000000),
        onSecondary = Color(0xFF000000),
        onTertiary = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFE2E8F0)
    ),
    // 4: Crimson Red AMOLED Black
    darkColorScheme(
        primary = Color(0xFFFB7185),
        secondary = Color(0xFFE11D48),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF270912),
        onPrimary = Color(0xFF000000),
        onSecondary = Color(0xFF000000),
        onTertiary = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFE2E8F0)
    ),
    // 5: Vibrant Teal AMOLED Black
    darkColorScheme(
        primary = Color(0xFF2DD4BF),
        secondary = Color(0xFF14B8A6),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF0A2220),
        onPrimary = Color(0xFF000000),
        onSecondary = Color(0xFF000000),
        onTertiary = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFE2E8F0)
    )
)

fun getCustomTypography(fontScale: Float = 1.0f, fontFamilyChoice: String = "Default"): Typography {
    val family = when (fontFamilyChoice) {
        "SansSerif" -> FontFamily.SansSerif
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    val scale = fontScale.coerceIn(0.85f, 1.35f)

    return Typography(
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (30 * scale).sp,
            lineHeight = (38 * scale).sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * scale).sp,
            lineHeight = (32 * scale).sp,
            letterSpacing = (-0.25).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (20 * scale).sp,
            lineHeight = (28 * scale).sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (18 * scale).sp,
            lineHeight = (26 * scale).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (15 * scale).sp,
            lineHeight = (22 * scale).sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.2.sp
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * scale).sp,
            lineHeight = (17 * scale).sp,
            letterSpacing = 0.25.sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (10.5 * scale).sp,
            lineHeight = (15 * scale).sp,
            letterSpacing = 0.4.sp
        )
    )
}

@Composable
fun MyApplicationTheme(
    themeMode: String = "System",
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColorIndex: Int = 0,
    fontScale: Float = 1.0f,
    fontFamilyChoice: String = "Default",
    content: @Composable () -> Unit
) {
    val safeIndex = accentColorIndex.coerceIn(0, AccentColorsLight.size - 1)
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "Dark" -> true
        "Black" -> true
        "Light" -> false
        else -> darkTheme || isSystemDark
    }
    val isBlack = themeMode == "Black"
    val colorScheme = when {
        isBlack -> AccentColorsBlack[safeIndex]
        isDark -> AccentColorsDark[safeIndex]
        else -> AccentColorsLight[safeIndex]
    }
    val typography = getCustomTypography(fontScale, fontFamilyChoice)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = (if (isBlack) AmoledBlack else colorScheme.primary).toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

@Composable
fun StoreManagerTheme(
    themeMode: String = "System",
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColorIndex: Int = 0,
    fontScale: Float = 1.0f,
    fontFamilyChoice: String = "Default",
    content: @Composable () -> Unit
) {
    MyApplicationTheme(
        themeMode = themeMode,
        darkTheme = darkTheme,
        accentColorIndex = accentColorIndex,
        fontScale = fontScale,
        fontFamilyChoice = fontFamilyChoice,
        content = content
    )
}
