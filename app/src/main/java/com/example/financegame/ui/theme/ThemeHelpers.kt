package com.example.financegame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color


// ======================== АВТОМАТИЧНІ КОЛЬОРИ ТЕКСТУ ========================

/**
 * Основний колір тексту - автоматично змінюється з темою
 * Використовується для заголовків та важливого тексту
 */
val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

/**
 * Вторинний колір тексту - автоматично змінюється з темою
 * Використовується для описів, підписів, менш важливого тексту
 */
val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

/**
 * Повертає колір для іконок залежно від теми
 */
val IconTint: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

/**
 * Повертає колір для розділювачів
 */
val DividerColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)

// ======================== КОМПОЗАБЛ ОБГОРТКИ ========================

/**
 * Універсальний композабл для тексту з автоматичним кольором
 * Замість звичайного Text використовуйте ThemedText
 */
@Composable
fun ThemedText(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    isPrimary: Boolean = true,  // true = основний текст, false = вторинний
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    androidx.compose.material3.Text(
        text = text,
        style = style,
        color = if (isPrimary) TextPrimary else TextSecondary,
        modifier = modifier,
        maxLines = maxLines
    )
}

// ======================== LEGACY SUPPORT (для сумісності) ========================


object LegacyColors {
    val TextPrimary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface

    val TextSecondary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val TextLight: Color
        get() = Color(0xFFFFFFFF)

    val TextDark: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface
}