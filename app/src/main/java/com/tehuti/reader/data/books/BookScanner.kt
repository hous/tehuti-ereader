package com.tehuti.reader.data.books

import androidx.documentfile.provider.DocumentFile

val SUPPORTED_BOOK_EXTENSIONS = setOf("epub")

object BookScanner {

    fun scan(root: DocumentFile): List<DocumentFile> {
        val results = mutableListOf<DocumentFile>()
        val pending = ArrayDeque<DocumentFile>()
        pending.add(root)

        while (pending.isNotEmpty()) {
            val dir = pending.removeFirst()
            for (child in dir.listFiles()) {
                when {
                    child.isDirectory -> pending.add(child)
                    child.isFile && isSupportedBook(child.name) -> results.add(child)
                }
            }
        }

        return results
    }

    private fun isSupportedBook(name: String?): Boolean {
        val extension = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        return extension in SUPPORTED_BOOK_EXTENSIONS
    }
}
