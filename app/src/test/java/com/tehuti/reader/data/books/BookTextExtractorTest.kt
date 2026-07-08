package com.tehuti.reader.data.books

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTextExtractorTest {

    private val spoilerMarker = "SPOILER_MARKER_BEYOND_BOUNDARY"

    @Test
    fun `includes prior chapters in full`() {
        val chapters = listOf("chapter zero.", "chapter one.", "chapter two, half read.")
        val result = BookTextExtractor.truncate(chapters, boundaryIndex = 2, boundaryProgression = 0.5, maxChars = 10_000)

        assertTrue(result.contains("chapter zero."))
        assertTrue(result.contains("chapter one."))
    }

    @Test
    fun `cuts the boundary chapter at the given progression, rounding down`() {
        val boundaryChapter = "0123456789" // 10 chars, easy to reason about
        val chapters = listOf(boundaryChapter)
        val result = BookTextExtractor.truncate(chapters, boundaryIndex = 0, boundaryProgression = 0.35, maxChars = 10_000)

        // floor(10 * 0.35) = 3
        assertEquals("012", result)
    }

    @Test
    fun `zero progression yields nothing from the boundary chapter`() {
        val chapters = listOf("prior chapter.", "boundary chapter never read at all.")
        val result = BookTextExtractor.truncate(chapters, boundaryIndex = 1, boundaryProgression = 0.0, maxChars = 10_000)

        assertTrue(result.contains("prior chapter."))
        assertFalse(result.contains("boundary chapter"))
    }

    @Test
    fun `full progression includes the entire boundary chapter`() {
        val boundaryChapter = "the whole boundary chapter."
        val chapters = listOf("prior.", boundaryChapter)
        val result = BookTextExtractor.truncate(chapters, boundaryIndex = 1, boundaryProgression = 1.0, maxChars = 10_000)

        assertTrue(result.endsWith(boundaryChapter))
    }

    @Test
    fun `never leaks content from chapters past the boundary index, even if present in the input`() {
        // Defense in depth: even if a caller mistakenly passed chapters beyond the boundary,
        // truncate() must never surface them. In real use BookTextExtractor.extractReadSoFar
        // never fetches these chapters at all, but this guards the pure function's own contract.
        val chapters = listOf(
            "chapter zero, fully read.",
            "chapter one, the boundary chapter, half read up to here",
            "chapter two, $spoilerMarker, never read.",
            "chapter three, $spoilerMarker, definitely never read.",
        )
        val result = BookTextExtractor.truncate(chapters, boundaryIndex = 1, boundaryProgression = 0.5, maxChars = 10_000)

        assertFalse(result.contains(spoilerMarker))
    }

    @Test
    fun `boundary chapter cutoff never includes text past its own progression point`() {
        val boundaryChapter = "safe text before the cut point." + spoilerMarker + "unsafe text after the cut point."
        val cutIndex = boundaryChapter.indexOf(spoilerMarker)
        val progression = cutIndex.toDouble() / boundaryChapter.length

        val result = BookTextExtractor.truncate(listOf(boundaryChapter), boundaryIndex = 0, boundaryProgression = progression, maxChars = 10_000)

        assertFalse(result.contains(spoilerMarker))
        assertTrue(result.contains("safe text before the cut point."))
    }

    @Test
    fun `sliding window keeps only the most recent text when over the character budget`() {
        val early = "EARLY_CONTENT_THAT_SHOULD_BE_DROPPED "
        val recent = "recent content that should survive the cap."
        val chapters = listOf(early, recent)
        val result = BookTextExtractor.truncate(chapters, boundaryIndex = 1, boundaryProgression = 1.0, maxChars = recent.length)

        assertEquals(recent, result)
        assertFalse(result.contains("EARLY_CONTENT_THAT_SHOULD_BE_DROPPED"))
    }

    @Test
    fun `empty chapter list produces empty output`() {
        val result = BookTextExtractor.truncate(emptyList(), boundaryIndex = 0, boundaryProgression = 0.5, maxChars = 100)
        assertEquals("", result)
    }
}
