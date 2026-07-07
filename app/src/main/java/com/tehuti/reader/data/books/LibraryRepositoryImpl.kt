package com.tehuti.reader.data.books

import android.net.Uri
import com.tehuti.reader.domain.model.Book
import com.tehuti.reader.domain.model.BookFormat
import com.tehuti.reader.domain.repo.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val booksDirectoryAccess: BooksDirectoryAccess,
) : LibraryRepository {

    override suspend fun scanBooks(treeUri: Uri): List<Book> = withContext(Dispatchers.IO) {
        val root = booksDirectoryAccess.documentFileFor(treeUri) ?: return@withContext emptyList()
        BookScanner.scan(root)
            .map { file ->
                Book(
                    id = file.uri.toString(),
                    sourceUri = file.uri.toString(),
                    format = BookFormat.EPUB,
                    title = file.name?.substringBeforeLast('.') ?: file.name.orEmpty(),
                )
            }
            .sortedBy { it.title.lowercase() }
    }
}
