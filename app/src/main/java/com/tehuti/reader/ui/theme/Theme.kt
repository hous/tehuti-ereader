package com.tehuti.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = TehutiAccentLight,
    onPrimary = TehutiPaperLight,
    background = TehutiPaperLight,
    onBackground = TehutiInkLight,
    surface = TehutiPaperLight,
    onSurface = TehutiInkLight,
)

private val DarkColors = darkColorScheme(
    primary = TehutiAccentDark,
    onPrimary = TehutiInkDark,
    background = TehutiPaperDark,
    onBackground = TehutiInkDark,
    surface = TehutiPaperDark,
    onSurface = TehutiInkDark,
)

@Composable
fun TehutiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TehutiTypography,
        content = content,
    )
}
