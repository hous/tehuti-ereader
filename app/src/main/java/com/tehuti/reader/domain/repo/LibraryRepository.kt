package com.tehuti.reader.domain.repo

import android.net.Uri
import com.tehuti.reader.domain.model.Book

interface LibraryRepository {
    suspend fun scanBooks(treeUri: Uri): List<Book>
}
