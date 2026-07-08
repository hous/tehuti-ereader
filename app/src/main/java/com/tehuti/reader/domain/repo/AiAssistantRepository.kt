package com.tehuti.reader.domain.repo

import com.tehuti.reader.domain.model.AiAvailability
import com.tehuti.reader.domain.model.AiGenerationResult
import kotlinx.coroutines.flow.Flow

/** Text-in/text-out on-device AI generation — no Readium/Publication knowledge here. */
interface AiAssistantRepository {
    suspend fun checkAvailability(): AiAvailability
    fun generate(prompt: String): Flow<AiGenerationResult>
}
