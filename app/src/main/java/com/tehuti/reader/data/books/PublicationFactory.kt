package com.tehuti.reader.data.books

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationParser = DefaultPublicationParser(context, httpClient, assetRetriever, null, emptyList())
    private val publicationOpener = PublicationOpener(publicationParser, emptyList()) { }

    suspend fun open(uri: Uri): Publication? {
        val url = AbsoluteUrl(uri.toString()) ?: return null
        val asset = assetRetriever.retrieve(url, FormatHints()).getOrNull() ?: return null
        return publicationOpener.open(asset, null, false, { }, null).getOrNull()
    }
}
