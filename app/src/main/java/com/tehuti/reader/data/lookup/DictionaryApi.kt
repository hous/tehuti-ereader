package com.tehuti.reader.data.lookup

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryApi {
    @GET("api/v2/entries/en/{word}")
    suspend fun getDefinitions(@Path("word") word: String): List<DictionaryEntryDto>
}

@Serializable
data class DictionaryEntryDto(
    val word: String,
    val phonetic: String? = null,
    val meanings: List<MeaningDto> = emptyList(),
)

@Serializable
data class MeaningDto(
    val partOfSpeech: String,
    val definitions: List<DefinitionDto> = emptyList(),
)

@Serializable
data class DefinitionDto(
    val definition: String,
)
