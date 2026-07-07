package com.tehuti.reader.data.books

import android.graphics.Bitmap
import android.net.Uri
import org.readium.r2.shared.publication.services.cover
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedMetadata(
    val title: String,
    val author: String?,
    val cover: Bitmap?,
)

@Singleton
class EpubMetadataExtractor @Inject constructor(
    private val publicationFactory: PublicationFactory,
) {
    suspend fun extract(uri: Uri): ExtractedMetadata? {
        val publication = publicationFactory.open(uri) ?: return null
        return try {
            ExtractedMetadata(
                title = publication.metadata.title ?: "Untitled",
                author = publication.metadata.authors.firstOrNull()?.localizedName?.string,
                cover = publication.cover(),
            )
        } finally {
            publication.close()
        }
    }
}
