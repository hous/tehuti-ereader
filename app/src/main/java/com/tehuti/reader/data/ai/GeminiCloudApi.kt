package com.tehuti.reader.data.ai

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiCloudApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateContentRequest,
    ): GeminiGenerateContentResponse
}

@Serializable
data class GeminiGenerateContentRequest(val contents: List<GeminiContentDto>)

@Serializable
data class GeminiContentDto(val parts: List<GeminiPartDto>)

@Serializable
data class GeminiPartDto(val text: String)

@Serializable
data class GeminiGenerateContentResponse(val candidates: List<GeminiCandidateDto> = emptyList())

@Serializable
data class GeminiCandidateDto(val content: GeminiContentDto? = null)
