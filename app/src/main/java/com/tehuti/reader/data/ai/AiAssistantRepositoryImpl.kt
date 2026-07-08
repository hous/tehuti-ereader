package com.tehuti.reader.data.ai

import com.tehuti.reader.domain.model.AiAvailability
import com.tehuti.reader.domain.model.AiGenerationResult
import com.tehuti.reader.domain.repo.AiAssistantRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AiAssistantRepositoryImpl @Inject constructor(
    private val geminiNanoClient: GeminiNanoClient,
) : AiAssistantRepository {

    override suspend fun checkAvailability(): AiAvailability = geminiNanoClient.checkStatus()

    override fun generate(prompt: String): Flow<AiGenerationResult> = geminiNanoClient.generateContent(prompt)
}
