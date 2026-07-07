package com.tehuti.reader.data.books

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedMetadata(
    val title: String,
    val author: String?,
    val cover: Bitmap?,
)

@Singleton
class EpubMetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationParser = DefaultPublicationParser(context, httpClient, assetRetriever, null, emptyList())
    private val publicationOpener = PublicationOpener(publicationParser, emptyList()) { }

    suspend fun extract(uri: Uri): ExtractedMetadata? {
        val url = AbsoluteUrl(uri.toString()) ?: return null
        val asset = assetRetriever.retrieve(url, FormatHints()).getOrNull() ?: return null
        val publication = publicationOpener.open(asset, null, false, { }, null).getOrNull() ?: return null
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
