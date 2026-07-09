package com.tehuti.reader.domain.model

enum class AppFontFamily(val cssValue: String, val displayName: String) {
    SERIF("serif", "Serif"),
    SANS_SERIF("sans-serif", "Sans Serif"),
    MONOSPACE("monospace", "Monospace"),
}

enum class AppTheme {
    LIGHT,
    DARK,
    SEPIA,
}

data class ReaderSettings(
    val fontSizePercent: Int = 100,
    val fontFamily: AppFontFamily = AppFontFamily.SERIF,
    val theme: AppTheme = AppTheme.LIGHT,
    // 0 = off, 1 = maximal (heavily dimmed, near-zero blue).
    val blueLightFilter: Float = 0f,
)
