package com.example.financegame.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = OceanBreezeColors.Error
)

private fun getOceanDark() = darkColorScheme(
    primary = OceanBreezeColors.Primary,
    secondary = OceanBreezeColors.Secondary,
    tertiary = OceanBreezeColors.Accent,
    background = OceanBreezeColors.BackgroundDark,
    surface = OceanBreezeColors.SurfaceDark,
    surfaceVariant = OceanBreezeColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = OceanBreezeColors.Error
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
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = SakuraDreamColors.Error
)

private fun getSakuraDark() = darkColorScheme(
    primary = SakuraDreamColors.Primary,
    secondary = SakuraDreamColors.Secondary,
    tertiary = SakuraDreamColors.Accent,
    background = SakuraDreamColors.BackgroundDark,
    surface = SakuraDreamColors.SurfaceDark,
    surfaceVariant = SakuraDreamColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = SakuraDreamColors.Error
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
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ForestMistColors.Error
)

private fun getForestDark() = darkColorScheme(
    primary = ForestMistColors.Primary,
    secondary = ForestMistColors.Secondary,
    tertiary = ForestMistColors.Accent,
    background = ForestMistColors.BackgroundDark,
    surface = ForestMistColors.SurfaceDark,
    surfaceVariant = ForestMistColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = ForestMistColors.Error
)

// Sunset Glow
private fun getSunsetLight() = lightColorScheme(
    primary = SunsetGlowColors.Primary,
    secondary = SunsetGlowColors.Secondary,
    tertiary = SunsetGlowColors.Accent,
    background = SunsetGlowColors.BackgroundLight,
    surface = SunsetGlowColors.SurfaceLight,
    surfaceVariant = SunsetGlowColors.CardLight,
    onPrimary = TextLight,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = SunsetGlowColors.Error
)

private fun getSunsetDark() = darkColorScheme(
    primary = SunsetGlowColors.Primary,
    secondary = SunsetGlowColors.Secondary,
    tertiary = SunsetGlowColors.Accent,
    background = SunsetGlowColors.BackgroundDark,
    surface = SunsetGlowColors.SurfaceDark,
    surfaceVariant = SunsetGlowColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = SunsetGlowColors.Error
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
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = MidnightPurpleColors.Error
)

private fun getMidnightDark() = darkColorScheme(
    primary = MidnightPurpleColors.Primary,
    secondary = MidnightPurpleColors.Secondary,
    tertiary = MidnightPurpleColors.Accent,
    background = MidnightPurpleColors.BackgroundDark,
    surface = MidnightPurpleColors.SurfaceDark,
    surfaceVariant = MidnightPurpleColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = MidnightPurpleColors.Error
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
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = IceCrystalColors.Error
)

private fun getIceDark() = darkColorScheme(
    primary = IceCrystalColors.Primary,
    secondary = IceCrystalColors.Secondary,
    tertiary = IceCrystalColors.Accent,
    background = IceCrystalColors.BackgroundDark,
    surface = IceCrystalColors.SurfaceDark,
    surfaceVariant = IceCrystalColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = IceCrystalColors.Error
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
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = LavaFlowColors.Error
)

private fun getLavaDark() = darkColorScheme(
    primary = LavaFlowColors.Primary,
    secondary = LavaFlowColors.Secondary,
    tertiary = LavaFlowColors.Accent,
    background = LavaFlowColors.BackgroundDark,
    surface = LavaFlowColors.SurfaceDark,
    surfaceVariant = LavaFlowColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = LavaFlowColors.Error
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
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = MoonlightColors.Error
)

private fun getMoonlightDark() = darkColorScheme(
    primary = MoonlightColors.Primary,
    secondary = MoonlightColors.Secondary,
    tertiary = MoonlightColors.Accent,
    background = MoonlightColors.BackgroundDark,
    surface = MoonlightColors.SurfaceDark,
    surfaceVariant = MoonlightColors.CardDark,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = MoonlightColors.Error
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