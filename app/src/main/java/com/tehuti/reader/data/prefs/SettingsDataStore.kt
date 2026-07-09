package com.tehuti.reader.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tehuti.reader.domain.model.AppFontFamily
import com.tehuti.reader.domain.model.AppTheme
import com.tehuti.reader.domain.model.LibrarySortOrder
import com.tehuti.reader.domain.model.ReaderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tehuti_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val BOOKS_TREE_URI = stringPreferencesKey("books_tree_uri")
        val FONT_SIZE_PERCENT = intPreferencesKey("font_size_percent")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val THEME = stringPreferencesKey("theme")
        val BLUE_LIGHT_FILTER = floatPreferencesKey("blue_light_filter")
        val LIBRARY_SORT_ORDER = stringPreferencesKey("library_sort_order")
    }

    val booksTreeUri: Flow<String?> = context.dataStore.data.map { it[Keys.BOOKS_TREE_URI] }

    suspend fun setBooksTreeUri(uri: String) {
        context.dataStore.edit { it[Keys.BOOKS_TREE_URI] = uri }
    }

    val readerSettings: Flow<ReaderSettings> = context.dataStore.data.map { prefs ->
        ReaderSettings(
            fontSizePercent = prefs[Keys.FONT_SIZE_PERCENT] ?: 100,
            fontFamily = prefs[Keys.FONT_FAMILY]?.let { runCatching { AppFontFamily.valueOf(it) }.getOrNull() }
                ?: AppFontFamily.SERIF,
            theme = prefs[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                ?: AppTheme.LIGHT,
            blueLightFilter = (prefs[Keys.BLUE_LIGHT_FILTER] ?: 0f).coerceIn(0f, 1f),
        )
    }

    suspend fun setFontSizePercent(percent: Int) {
        context.dataStore.edit { it[Keys.FONT_SIZE_PERCENT] = percent }
    }

    suspend fun setFontFamily(fontFamily: AppFontFamily) {
        context.dataStore.edit { it[Keys.FONT_FAMILY] = fontFamily.name }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setBlueLightFilter(level: Float) {
        context.dataStore.edit { it[Keys.BLUE_LIGHT_FILTER] = level.coerceIn(0f, 1f) }
    }

    val librarySortOrder: Flow<LibrarySortOrder> = context.dataStore.data.map { prefs ->
        prefs[Keys.LIBRARY_SORT_ORDER]?.let { runCatching { LibrarySortOrder.valueOf(it) }.getOrNull() }
            ?: LibrarySortOrder.LAST_READ
    }

    suspend fun setLibrarySortOrder(order: LibrarySortOrder) {
        context.dataStore.edit { it[Keys.LIBRARY_SORT_ORDER] = order.name }
    }
}
