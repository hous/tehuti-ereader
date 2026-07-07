package com.tehuti.reader.domain.model

enum class LookupType {
    DICTIONARY,
    WIKIPEDIA,
}

sealed interface LookupResult {
    data class Dictionary(
        val word: String,
        val phonetic: String?,
        val meanings: List<Meaning>,
    ) : LookupResult {
        data class Meaning(val partOfSpeech: String, val definitions: List<String>)
    }

    data class Wikipedia(
        val title: String,
        val extract: String,
        val thumbnailUrl: String?,
        val pageUrl: String?,
    ) : LookupResult
}
