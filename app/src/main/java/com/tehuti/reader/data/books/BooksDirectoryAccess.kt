package com.tehuti.reader.data.books

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.tehuti.reader.data.prefs.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BooksDirectoryAccess @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
) {
    val booksTreeUri: Flow<Uri?> = settingsDataStore.booksTreeUri.map { it?.toUri() }

    suspend fun persistTreeUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        settingsDataStore.setBooksTreeUri(uri.toString())
    }

    fun documentFileFor(treeUri: Uri): DocumentFile? = DocumentFile.fromTreeUri(context, treeUri)

    fun displayNameFor(treeUri: Uri): String? = documentFileFor(treeUri)?.name
}
