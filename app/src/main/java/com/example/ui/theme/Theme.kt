package com.example.ui.theme

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

data class AccentColorOption(
    val key: String,
    val name: String,
    val previewColor: Color,
    val primaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val primaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val secondaryLight: Color,
    val secondaryContainerLight: Color
)

val AccentColors = listOf(
    AccentColorOption(
        key = "BLUE",
        name = "Royal Blue",
        previewColor = Color(0xFF2563EB),
        primaryLight = Color(0xFF2563EB),
        primaryContainerLight = Color(0xFFEFF6FF),
        onPrimaryContainerLight = Color(0xFF1D4ED8),
        primaryDark = Color(0xFF60A5FA),
        primaryContainerDark = Color(0xFF1E3A8A),
        onPrimaryContainerDark = Color(0xFFDBEAFE),
        secondaryLight = Color(0xFF3B82F6),
        secondaryContainerLight = Color(0xFFDBEAFE)
    ),
    AccentColorOption(
        key = "EMERALD",
        name = "Emerald Green",
        previewColor = Color(0xFF059669),
        primaryLight = Color(0xFF059669),
        primaryContainerLight = Color(0xFFECFDF5),
        onPrimaryContainerLight = Color(0xFF047857),
        primaryDark = Color(0xFF34D399),
        primaryContainerDark = Color(0xFF064E3B),
        onPrimaryContainerDark = Color(0xFFD1FAE5),
        secondaryLight = Color(0xFF10B981),
        secondaryContainerLight = Color(0xFFD1FAE5)
    ),
    AccentColorOption(
        key = "PURPLE",
        name = "Purple Violet",
        previewColor = Color(0xFF7C3AED),
        primaryLight = Color(0xFF7C3AED),
        primaryContainerLight = Color(0xFFF5F3FF),
        onPrimaryContainerLight = Color(0xFF6D28D9),
        primaryDark = Color(0xFFA78BFA),
        primaryContainerDark = Color(0xFF4C1D95),
        onPrimaryContainerDark = Color(0xFFEDE9FE),
        secondaryLight = Color(0xFF8B5CF6),
        secondaryContainerLight = Color(0xFFEDE9FE)
    ),
    AccentColorOption(
        key = "INDIGO",
        name = "Deep Indigo",
        previewColor = Color(0xFF4F46E5),
        primaryLight = Color(0xFF4F46E5),
        primaryContainerLight = Color(0xFFEEF2FF),
        onPrimaryContainerLight = Color(0xFF4338CA),
        primaryDark = Color(0xFF818CF8),
        primaryContainerDark = Color(0xFF312E81),
        onPrimaryContainerDark = Color(0xFFE0E7FF),
        secondaryLight = Color(0xFF6366F1),
        secondaryContainerLight = Color(0xFFE0E7FF)
    ),
    AccentColorOption(
        key = "AMBER",
        name = "Amber Gold",
        previewColor = Color(0xFFD97706),
        primaryLight = Color(0xFFD97706),
        primaryContainerLight = Color(0xFFFFFBEB),
        onPrimaryContainerLight = Color(0xFFB45309),
        primaryDark = Color(0xFFFBBF24),
        primaryContainerDark = Color(0xFF78350F),
        onPrimaryContainerDark = Color(0xFFFEF3C7),
        secondaryLight = Color(0xFFF59E0B),
        secondaryContainerLight = Color(0xFFFEF3C7)
    ),
    AccentColorOption(
        key = "ROSE",
        name = "Rose Red",
        previewColor = Color(0xFFE11D48),
        primaryLight = Color(0xFFE11D48),
        primaryContainerLight = Color(0xFFFFF1F2),
        onPrimaryContainerLight = Color(0xFFBE123C),
        primaryDark = Color(0xFFFB7185),
        primaryContainerDark = Color(0xFF881337),
        onPrimaryContainerDark = Color(0xFFFFE4E6),
        secondaryLight = Color(0xFFF43F5E),
        secondaryContainerLight = Color(0xFFFFE4E6)
    ),
    AccentColorOption(
        key = "TEAL",
        name = "Teal Cyan",
        previewColor = Color(0xFF0D9488),
        primaryLight = Color(0xFF0D9488),
        primaryContainerLight = Color(0xFFF0FDFA),
        onPrimaryContainerLight = Color(0xFF0F766E),
        primaryDark = Color(0xFF2DD4BF),
        primaryContainerDark = Color(0xFF134E4A),
        onPrimaryContainerDark = Color(0xFFCCFBF1),
        secondaryLight = Color(0xFF14B8A6),
        secondaryContainerLight = Color(0xFFCCFBF1)
    )
)

@Composable
fun StoreManagerTheme(
    themeMode: String = "SYSTEM",
    accentColorName: String = "BLUE",
    darkTheme: Boolean = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = AccentColors.find { it.key.equals(accentColorName, ignoreCase = true) }
        ?: AccentColors.first()

    val lightColorScheme = lightColorScheme(
        primary = palette.primaryLight,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainerLight,
        onPrimaryContainer = palette.onPrimaryContainerLight,
        secondary = palette.secondaryLight,
        onSecondary = Color.White,
        secondaryContainer = palette.secondaryContainerLight,
        onSecondaryContainer = palette.primaryLight,
        tertiary = BentoAmber,
        onTertiary = Color.White,
        tertiaryContainer = BentoAmberLight,
        onTertiaryContainer = Color(0xFF78350F),
        background = BentoBg,
        onBackground = BentoCardDark,
        surface = BentoCardBg,
        onSurface = BentoCardDark,
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Slate700,
        outline = BentoBorder,
        error = BentoRose,
        onError = Color.White,
        errorContainer = BentoRoseLight,
        onErrorContainer = Color(0xFF881337)
    )

    val darkColorScheme = darkColorScheme(
        primary = palette.primaryDark,
        onPrimary = Color(0xFF0F172A),
        primaryContainer = palette.primaryContainerDark,
        onPrimaryContainer = palette.onPrimaryContainerDark,
        secondary = palette.primaryDark,
        onSecondary = Color(0xFF0F172A),
        secondaryContainer = palette.primaryContainerDark,
        onSecondaryContainer = palette.onPrimaryContainerDark,
        tertiary = Color(0xFFFBBF24),
        onTertiary = Color(0xFF451A03),
        background = Slate900,
        onBackground = Slate50,
        surface = Slate800,
        onSurface = Slate50,
        surfaceVariant = Slate700,
        onSurfaceVariant = Slate200,
        outline = Slate600,
        error = Color(0xFFF87171),
        onError = Color(0xFF7F1D1D)
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
