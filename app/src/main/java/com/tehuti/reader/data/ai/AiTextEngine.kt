package com.tehuti.reader.data.ai

/**
 * A single text-in/text-out generation backend. Implementations throw on failure — callers
 * (see [AiAssistantRepositoryImpl]) are responsible for catching and mapping to a domain result,
 * and for deciding which engine to use for a given request.
 */
interface AiTextEngine {
    suspend fun generateResponse(prompt: String): String
}
