package com.tehuti.reader.data.lookup

import com.tehuti.reader.domain.model.LookupResult
import com.tehuti.reader.domain.repo.LookupRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import javax.inject.Inject

class LookupRepositoryImpl @Inject constructor(
    private val dictionaryApi: DictionaryApi,
    private val wikipediaApi: WikipediaApi,
) : LookupRepository {

    override suspend fun lookupDictionary(word: String): LookupResult.Dictionary? {
        return try {
            val entry = dictionaryApi.getDefinitions(word.trim().lowercase()).firstOrNull() ?: return null
            LookupResult.Dictionary(
                word = entry.word,
                phonetic = entry.phonetic,
                meanings = entry.meanings.map { meaning ->
                    LookupResult.Dictionary.Meaning(
                        partOfSpeech = meaning.partOfSpeech,
                        definitions = meaning.definitions.map { it.definition },
                    )
                },
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun lookupWikipedia(term: String): LookupResult.Wikipedia? {
        val trimmed = term.trim()
        val summary = try {
            wikipediaApi.getSummary(trimmed)
        } catch (e: HttpException) {
            if (e.code() == 404) fallbackSummary(trimmed) else return null
        } catch (e: Exception) {
            return null
        } ?: return null

        return LookupResult.Wikipedia(
            title = summary.title,
            extract = summary.extract,
            thumbnailUrl = summary.thumbnail?.source,
            pageUrl = summary.contentUrls?.desktop?.page,
        )
    }

    private suspend fun fallbackSummary(term: String): WikipediaSummaryDto? {
        return try {
            val results = wikipediaApi.openSearch(term)
            val titlesArray = results.getOrNull(1) as? JsonArray
            val fallbackTitle = titlesArray?.firstOrNull()?.jsonPrimitive?.content ?: return null
            wikipediaApi.getSummary(fallbackTitle)
        } catch (e: Exception) {
            null
        }
    }
}
