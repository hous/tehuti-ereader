package com.tehuti.reader.data.books

import org.jsoup.Jsoup
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.data.decodeString
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.use
import kotlin.math.floor

/**
 * Extracts the plain text of a book from its beginning up to a boundary [Locator], for feeding
 * to the on-device AI companion. Spine items past the boundary are never read at all — not
 * fetched-and-discarded, genuinely untouched — since this is the sole source of the spoiler
 * boundary guarantee: nothing unread ever reaches the model.
 */
object BookTextExtractor {

    // Gemini Nano's on-device context window is much smaller than a cloud model's. These caps
    // are a sliding window ending exactly at the boundary (never before it truncated away), so a
    // reader most of the way through a long novel gets a "recent context" excerpt rather than the
    // whole book so far — an accepted tradeoff of recomputing from scratch with no rolling cache.
    const val SUMMARY_MAX_CHARS = 6_000
    const val EXPLAIN_MAX_CHARS = 3_000

    suspend fun extractReadSoFar(publication: Publication, boundary: Locator?, maxChars: Int): String {
        if (boundary == null) return ""
        val readingOrder = publication.readingOrder
        val boundaryIndex = readingOrder.indexOfFirst { it.url() == boundary.href }
        // Fail closed: if the boundary can't be resolved, don't guess how much is "read."
        if (boundaryIndex == -1) return ""

        val progression = (boundary.locations.progression ?: 0.0).coerceIn(0.0, 1.0)
        val chapterTexts = (0..boundaryIndex).map { index ->
            val link = readingOrder[index]
            val html = publication.get(link)?.use { resource ->
                resource.read().flatMap { it.decodeString() }.getOrNull()
            }
            html?.let { Jsoup.parse(it).text() } ?: ""
        }

        return truncate(chapterTexts, boundaryIndex, progression, maxChars)
    }

    /**
     * Pure boundary-truncation logic, kept free of Readium/Jsoup so it's unit-testable on its
     * own: [chapterTexts] must contain exactly the chapters from index 0 to [boundaryIndex]
     * inclusive (never anything past it). The chapter at [boundaryIndex] is cut at
     * [boundaryProgression] through its length, rounding down so it can only remove content,
     * never include more than what's confirmed read.
     */
    internal fun truncate(
        chapterTexts: List<String>,
        boundaryIndex: Int,
        boundaryProgression: Double,
        maxChars: Int,
    ): String {
        val builder = StringBuilder()
        for (index in chapterTexts.indices) {
            val text = chapterTexts[index]
            when {
                index < boundaryIndex -> builder.append(text).append("\n\n")
                index == boundaryIndex -> {
                    val cut = floor(text.length * boundaryProgression).toInt().coerceIn(0, text.length)
                    builder.append(text, 0, cut)
                }
                // index > boundaryIndex is unreachable in practice — chapterTexts should never
                // contain anything past the boundary — but is ignored here regardless.
            }
        }
        val full = builder.toString()
        return if (full.length <= maxChars) full else full.takeLast(maxChars)
    }
}
