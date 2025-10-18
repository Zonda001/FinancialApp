package com.example.financegame.ui.theme

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

// Функція для отримання ColorScheme на основі теми
@Composable
fun getColorScheme(themeName: String, isDark: Boolean) = when (themeName) {
    "sakura" -> if (isDark) getSakuraDark() else getSakuraLight()
    "forest" -> if (isDark) getForestDark() else getForestLight()
    "sunset" -> if (isDark) getSunsetDark() else getSunsetLight()
    "midnight" -> if (isDark) getMidnightDark() else getMidnightLight()
    "ice" -> if (isDark) getIceDark() else getIceLight()
    "lava" -> if (isDark) getLavaDark() else getLavaLight()
    "moonlight" -> if (isDark) getMoonlightDark() else getMoonlightLight()
    else -> if (isDark) getOceanDark() else getOceanLight()
}

// Ocean Breeze
private fun getOceanLight() = lightColorScheme(
    primary = OceanBreezeColors.Primary,
    secondary = OceanBreezeColors.Secondary,
    tertiary = OceanBreezeColors.Accent,
    background = OceanBreezeColors.BackgroundLight,
    surface = OceanBreezeColors.SurfaceLight,
    surfaceVariant = OceanBreezeColors.CardLight,
    onPrimary = TextLight,
    onSecondary = Color(0xFF0C4A6E),        // Темний текст для акцентів
    onBackground = Color(0xFF0C4A6E),       // Темний синій текст на світлому фоні
    onSurface = Color(0xFF0C4A6E),          // Темний текст на поверхнях
    onSurfaceVariant = Color(0xFF0369A1),   // Середній синій для карток
    error = OceanBreezeColors.Error,
    onError = TextLight
)

private fun getOceanDark() = darkColorScheme(
    primary = OceanBreezeColors.Primary,
    secondary = OceanBreezeColors.Secondary,
    tertiary = OceanBreezeColors.Accent,
    background = OceanBreezeColors.BackgroundDark,
    surface = OceanBreezeColors.SurfaceDark,
    surfaceVariant = OceanBreezeColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = Color(0xFFE0F2FE),
    onSurface = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFFBAE6FD),
    error = OceanBreezeColors.Error,
    onError = TextLight
)

// Sakura Dream
private fun getSakuraLight() = lightColorScheme(
    primary = SakuraDreamColors.Primary,
    secondary = SakuraDreamColors.Secondary,
    tertiary = SakuraDreamColors.Accent,
    background = SakuraDreamColors.BackgroundLight,
    surface = SakuraDreamColors.SurfaceLight,
    surfaceVariant = SakuraDreamColors.CardLight,
    onPrimary = TextLight,
    onSecondary = Color(0xFF831843),
    onBackground = Color(0xFF831843),       // Темно-рожевий текст
    onSurface = Color(0xFF831843),
    onSurfaceVariant = Color(0xFF9F1239),   // Середній рожевий для карток
    error = SakuraDreamColors.Error,
    onError = TextLight
)

private fun getSakuraDark() = darkColorScheme(
    primary = SakuraDreamColors.Primary,
    secondary = SakuraDreamColors.Secondary,
    tertiary = SakuraDreamColors.Accent,
    background = SakuraDreamColors.BackgroundDark,
    surface = SakuraDreamColors.SurfaceDark,
    surfaceVariant = SakuraDreamColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = Color(0xFFFCE7F3),       // Світлий рожевий текст
    onSurface = Color(0xFFFCE7F3),
    onSurfaceVariant = Color(0xFFFBCFE8),
    error = SakuraDreamColors.Error,
    onError = TextLight
)

// Forest Mist
private fun getForestLight() = lightColorScheme(
    primary = ForestMistColors.Primary,
    secondary = ForestMistColors.Secondary,
    tertiary = ForestMistColors.Accent,
    background = ForestMistColors.BackgroundLight,
    surface = ForestMistColors.SurfaceLight,
    surfaceVariant = ForestMistColors.CardLight,
    onPrimary = TextLight,
    onSecondary = Color(0xFF065F46),
    onBackground = Color(0xFF065F46),       // Темно-зелений текст
    onSurface = Color(0xFF065F46),
    onSurfaceVariant = Color(0xFF047857),
    error = ForestMistColors.Error,
    onError = TextLight
)

private fun getForestDark() = darkColorScheme(
    primary = ForestMistColors.Primary,
    secondary = ForestMistColors.Secondary,
    tertiary = ForestMistColors.Accent,
    background = ForestMistColors.BackgroundDark,
    surface = ForestMistColors.SurfaceDark,
    surfaceVariant = ForestMistColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = Color(0xFFD1FAE5),       // Світлий зелений текст
    onSurface = Color(0xFFD1FAE5),
    onSurfaceVariant = Color(0xFFA7F3D0),
    error = ForestMistColors.Error,
    onError = TextLight
)

// Sunset Glow
private fun getSunsetLight() = lightColorScheme(
    primary = SunsetGlowColors.Primary,
    secondary = SunsetGlowColors.Secondary,
    tertiary = SunsetGlowColors.Accent,
    background = SunsetGlowColors.BackgroundLight,
    surface = SunsetGlowColors.SurfaceLight,
    surfaceVariant = SunsetGlowColors.CardLight,
    onPrimary = Color(0xFF78350F),          // Темний текст на золотому
    onSecondary = Color(0xFF78350F),
    onBackground = Color(0xFF78350F),       // Темно-коричневий текст
    onSurface = Color(0xFF78350F),
    onSurfaceVariant = Color(0xFF92400E),
    error = SunsetGlowColors.Error,
    onError = TextLight
)

private fun getSunsetDark() = darkColorScheme(
    primary = SunsetGlowColors.Primary,
    secondary = SunsetGlowColors.Secondary,
    tertiary = SunsetGlowColors.Accent,
    background = SunsetGlowColors.BackgroundDark,
    surface = SunsetGlowColors.SurfaceDark,
    surfaceVariant = SunsetGlowColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = Color(0xFFFEF3C7),       // Світлий золотий текст
    onSurface = Color(0xFFFEF3C7),
    onSurfaceVariant = Color(0xFFFDE68A),
    error = SunsetGlowColors.Error,
    onError = TextLight
)

// Midnight Purple
private fun getMidnightLight() = lightColorScheme(
    primary = MidnightPurpleColors.Primary,
    secondary = MidnightPurpleColors.Secondary,
    tertiary = MidnightPurpleColors.Accent,
    background = MidnightPurpleColors.BackgroundLight,
    surface = MidnightPurpleColors.SurfaceLight,
    surfaceVariant = MidnightPurpleColors.CardLight,
    onPrimary = TextLight,
    onSecondary = Color(0xFF581C87),
    onBackground = Color(0xFF581C87),       // Темно-фіолетовий текст
    onSurface = Color(0xFF581C87),
    onSurfaceVariant = Color(0xFF6B21A8),
    error = MidnightPurpleColors.Error,
    onError = TextLight
)

private fun getMidnightDark() = darkColorScheme(
    primary = MidnightPurpleColors.Primary,
    secondary = MidnightPurpleColors.Secondary,
    tertiary = MidnightPurpleColors.Accent,
    background = MidnightPurpleColors.BackgroundDark,
    surface = MidnightPurpleColors.SurfaceDark,
    surfaceVariant = MidnightPurpleColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = Color(0xFFEDE9FE),       // Світлий фіолетовий текст
    onSurface = Color(0xFFEDE9FE),
    onSurfaceVariant = Color(0xFFDDD6FE),
    error = MidnightPurpleColors.Error,
    onError = TextLight
)

// Ice Crystal
private fun getIceLight() = lightColorScheme(
    primary = IceCrystalColors.Primary,
    secondary = IceCrystalColors.Secondary,
    tertiary = IceCrystalColors.Accent,
    background = IceCrystalColors.BackgroundLight,
    surface = IceCrystalColors.SurfaceLight,
    surfaceVariant = IceCrystalColors.CardLight,
    onPrimary = TextLight,
    onSecondary = Color(0xFF164E63),
    onBackground = Color(0xFF164E63),       // Темно-синій текст
    onSurface = Color(0xFF164E63),
    onSurfaceVariant = Color(0xFF155E75),
    error = IceCrystalColors.Error,
    onError = TextLight
)

private fun getIceDark() = darkColorScheme(
    primary = IceCrystalColors.Primary,
    secondary = IceCrystalColors.Secondary,
    tertiary = IceCrystalColors.Accent,
    background = IceCrystalColors.BackgroundDark,
    surface = IceCrystalColors.SurfaceDark,
    surfaceVariant = IceCrystalColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = Color(0xFFCFFAFE),       // Світлий крижаний текст
    onSurface = Color(0xFFCFFAFE),
    onSurfaceVariant = Color(0xFFA5F3FC),
    error = IceCrystalColors.Error,
    onError = TextLight
)

// Lava Flow
private fun getLavaLight() = lightColorScheme(
    primary = LavaFlowColors.Primary,
    secondary = LavaFlowColors.Secondary,
    tertiary = LavaFlowColors.Accent,
    background = LavaFlowColors.BackgroundLight,
    surface = LavaFlowColors.SurfaceLight,
    surfaceVariant = LavaFlowColors.CardLight,
    onPrimary = TextLight,
    onSecondary = Color(0xFF7F1D1D),
    onBackground = Color(0xFF7F1D1D),       // Темно-червоний текст
    onSurface = Color(0xFF7F1D1D),
    onSurfaceVariant = Color(0xFF991B1B),
    error = LavaFlowColors.Error,
    onError = TextLight
)

private fun getLavaDark() = darkColorScheme(
    primary = LavaFlowColors.Primary,
    secondary = LavaFlowColors.Secondary,
    tertiary = LavaFlowColors.Accent,
    background = LavaFlowColors.BackgroundDark,
    surface = LavaFlowColors.SurfaceDark,
    surfaceVariant = LavaFlowColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = Color(0xFFFEE2E2),       // Світлий червоний текст
    onSurface = Color(0xFFFEE2E2),
    onSurfaceVariant = Color(0xFFFECACA),
    error = LavaFlowColors.Error,
    onError = TextLight
)

// Moonlight
private fun getMoonlightLight() = lightColorScheme(
    primary = MoonlightColors.Primary,
    secondary = MoonlightColors.Secondary,
    tertiary = MoonlightColors.Accent,
    background = MoonlightColors.BackgroundLight,
    surface = MoonlightColors.SurfaceLight,
    surfaceVariant = MoonlightColors.CardLight,
    onPrimary = TextLight,
    onSecondary = Color(0xFF3730A3),
    onBackground = Color(0xFF3730A3),       // Темно-індиго текст
    onSurface = Color(0xFF3730A3),
    onSurfaceVariant = Color(0xFF4338CA),
    error = MoonlightColors.Error,
    onError = TextLight
)

private fun getMoonlightDark() = darkColorScheme(
    primary = MoonlightColors.Primary,
    secondary = MoonlightColors.Secondary,
    tertiary = MoonlightColors.Accent,
    background = MoonlightColors.BackgroundDark,
    surface = MoonlightColors.SurfaceDark,
    surfaceVariant = MoonlightColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = Color(0xFFE0E7FF),       // Світлий індиго текст
    onSurface = Color(0xFFE0E7FF),
    onSurfaceVariant = Color(0xFFC7D2FE),
    error = MoonlightColors.Error,
    onError = TextLight
)

@Composable
fun FinanceGameTheme(
    themeName: String = "ocean",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(themeName, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}