package com.tehuti.reader.data.prefs

import com.tehuti.reader.domain.model.AppFontFamily
import com.tehuti.reader.domain.model.AppTheme
import com.tehuti.reader.domain.model.LibrarySortOrder
import com.tehuti.reader.domain.model.ReaderSettings
import com.tehuti.reader.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : SettingsRepository {

    override val settings: Flow<ReaderSettings> = settingsDataStore.readerSettings

    override suspend fun setFontSizePercent(percent: Int) {
        settingsDataStore.setFontSizePercent(percent)
    }

    override suspend fun setFontFamily(fontFamily: AppFontFamily) {
        settingsDataStore.setFontFamily(fontFamily)
    }

    override suspend fun setTheme(theme: AppTheme) {
        settingsDataStore.setTheme(theme)
    }

    override val librarySortOrder: Flow<LibrarySortOrder> = settingsDataStore.librarySortOrder

    override suspend fun setLibrarySortOrder(order: LibrarySortOrder) {
        settingsDataStore.setLibrarySortOrder(order)
    }
}
