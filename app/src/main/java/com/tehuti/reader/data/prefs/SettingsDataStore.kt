package com.tehuti.reader.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    }

    val booksTreeUri: Flow<String?> = context.dataStore.data.map { it[Keys.BOOKS_TREE_URI] }

    suspend fun setBooksTreeUri(uri: String) {
        context.dataStore.edit { it[Keys.BOOKS_TREE_URI] = uri }
    }
}
