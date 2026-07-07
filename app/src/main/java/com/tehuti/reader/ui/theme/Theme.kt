package com.tehuti.reader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.tehuti.reader.domain.model.AppTheme

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

private val SepiaColors = lightColorScheme(
    primary = TehutiAccentSepia,
    onPrimary = TehutiPaperSepia,
    background = TehutiPaperSepia,
    onBackground = TehutiInkSepia,
    surface = TehutiPaperSepia,
    onSurface = TehutiInkSepia,
)

@Composable
fun TehutiTheme(
    theme: AppTheme = AppTheme.LIGHT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (theme) {
        AppTheme.LIGHT -> LightColors
        AppTheme.DARK -> DarkColors
        AppTheme.SEPIA -> SepiaColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TehutiTypography,
        content = content,
    )
}
