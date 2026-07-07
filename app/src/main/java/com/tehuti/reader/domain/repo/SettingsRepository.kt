package com.tehuti.reader.domain.repo

import com.tehuti.reader.domain.model.AppFontFamily
import com.tehuti.reader.domain.model.AppTheme
import com.tehuti.reader.domain.model.ReaderSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<ReaderSettings>
    suspend fun setFontSizePercent(percent: Int)
    suspend fun setFontFamily(fontFamily: AppFontFamily)
    suspend fun setTheme(theme: AppTheme)
}
