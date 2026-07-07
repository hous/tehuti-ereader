package com.tehuti.reader.domain.repo

import android.net.Uri
import com.tehuti.reader.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeBooks(): Flow<List<Book>>
    suspend fun rescan(treeUri: Uri)
}
