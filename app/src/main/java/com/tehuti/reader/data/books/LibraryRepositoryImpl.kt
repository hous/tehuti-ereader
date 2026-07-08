package com.tehuti.reader.data.books

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tehuti.reader.data.local.BookDao
import com.tehuti.reader.data.local.BookEntity
import com.tehuti.reader.data.local.BookWithProgression
import com.tehuti.reader.domain.model.Book
import com.tehuti.reader.domain.model.BookFormat
import com.tehuti.reader.domain.repo.LibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val booksDirectoryAccess: BooksDirectoryAccess,
    private val bookDao: BookDao,
    private val metadataExtractor: EpubMetadataExtractor,
    @ApplicationContext private val context: Context,
) : LibraryRepository {

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAllWithProgression().map { rows -> rows.map { it.toDomain() } }

    override suspend fun rescan(treeUri: Uri) = withContext(Dispatchers.IO) {
        val root = booksDirectoryAccess.documentFileFor(treeUri) ?: return@withContext
        val scannedFiles = BookScanner.scan(root)
        val scannedIds = scannedFiles.associateBy { bookIdFor(it.uri.toString()) }
        val existingEntities = bookDao.getAll()
        val existingIds = existingEntities.map { it.id }.toSet()

        val staleEntities = existingEntities.filter { it.id !in scannedIds.keys }
        for (entity in staleEntities) {
            entity.coverPath?.let { File(it).delete() }
            bookDao.deleteById(entity.id)
        }

        val newEntries = scannedIds.filterKeys { it !in existingIds }
        for ((id, file) in newEntries) {
            try {
                val metadata = metadataExtractor.extract(file.uri)
                val coverPath = metadata?.cover?.let { saveCover(id, it) }
                bookDao.upsert(
                    BookEntity(
                        id = id,
                        sourceUri = file.uri.toString(),
                        format = BookFormat.EPUB.name,
                        title = metadata?.title ?: file.name?.substringBeforeLast('.') ?: "Untitled",
                        author = metadata?.author,
                        coverPath = coverPath,
                        addedAt = System.currentTimeMillis(),
                        lastOpenedAt = null,
                    ),
                )
            } catch (e: Exception) {
                bookDao.upsert(
                    BookEntity(
                        id = id,
                        sourceUri = file.uri.toString(),
                        format = BookFormat.EPUB.name,
                        title = file.name?.substringBeforeLast('.') ?: "Untitled",
                        author = null,
                        coverPath = null,
                        addedAt = System.currentTimeMillis(),
                        lastOpenedAt = null,
                    ),
                )
            }
        }
    }

    private fun saveCover(bookId: String, bitmap: Bitmap): String {
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val file = File(coversDir, "$bookId.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, out) }
        return file.absolutePath
    }
}

private fun BookWithProgression.toDomain(): Book = Book(
    id = book.id,
    sourceUri = book.sourceUri,
    format = BookFormat.valueOf(book.format),
    title = book.title,
    author = book.author,
    coverPath = book.coverPath,
    progression = progression,
    addedAt = book.addedAt,
    lastOpenedAt = book.lastOpenedAt,
)
