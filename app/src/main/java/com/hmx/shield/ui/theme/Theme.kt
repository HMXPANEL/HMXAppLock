package com.hmx.shield.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.hmx.shield.core.ThemeController
import com.hmx.shield.core.model.AccentColor
import com.hmx.shield.core.model.ThemeMode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

// Design tokens (from the Design System specification)
object Tokens {
    val BgPrimaryDark = Color(0xFF0B0B0F)
    val BgSecondaryDark = Color(0xFF13131A)
    val CardDark = Color(0xFF1A1B22)
    val BorderDark = Color(0xFF2A2C35)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB8BCC8)

    val GradientStart = Color(0xFF8B5CF6)
    val GradientEnd = Color(0xFF3B82F6)
}

data class ThemeState(val mode: ThemeMode, val accent: AccentColor)

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("No ThemeController provided")
}

private fun buildScheme(dark: Boolean, accentHex: String): ColorScheme {
    val accent = Color(AndroidColor.parseColor(accentHex))
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = Color.White,
        secondary = Color(0xFF3B82F6),
        background = if (dark) Tokens.BgPrimaryDark else Color.White,
        surface = if (dark) Tokens.CardDark else Color(0xFFF2F3F7),
        onBackground = if (dark) Tokens.TextPrimary else Color(0xFF101014),
        onSurface = if (dark) Tokens.TextPrimary else Color(0xFF101014),
        surfaceVariant = if (dark) Tokens.BgSecondaryDark else Color(0xFFE7E9F0),
        outline = if (dark) Tokens.BorderDark else Color(0xFFD5D8E0)
    )
}

@Composable
fun HmxTheme(
    content: @Composable () -> Unit
) {
    val controller = LocalThemeController.current
    val state by controller.state.collectAsState()
    val isDark = when (state.mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme = buildScheme(isDark, state.accent.hex)
    MaterialTheme(
        colorScheme = scheme,
        typography = HmxTypography,
        content = content
    )
}

@Composable
@ReadOnlyComposable
fun accentGradient(): List<Color> = listOf(Tokens.GradientStart, Tokens.GradientEnd)
