package com.tehuti.reader.data.ai

import com.tehuti.reader.domain.model.AiAvailability
import com.tehuti.reader.domain.model.AiGenerationResult
import com.tehuti.reader.domain.repo.AiAssistantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Routes each request to whichever [AiTextEngine] can actually serve it: [LocalNanoEngine] when
 * on-device AI is immediately available, otherwise [CloudFallbackEngine] — so the feature works
 * on the emulator and on devices/OS versions AICore doesn't support, not just on production
 * hardware. Neither engine call site here knows about Readium/Publication; extraction happens
 * upstream in [com.tehuti.reader.data.books.BookTextExtractor].
 */
class AiAssistantRepositoryImpl @Inject constructor(
    private val localNanoEngine: LocalNanoEngine,
    private val cloudFallbackEngine: CloudFallbackEngine,
) : AiAssistantRepository {

    override suspend fun checkAvailability(): AiAvailability {
        if (localNanoEngine.checkStatus() == AiAvailability.AVAILABLE) return AiAvailability.AVAILABLE
        return if (cloudFallbackEngine.isConfigured()) AiAvailability.AVAILABLE else AiAvailability.UNAVAILABLE
    }

    override fun generate(prompt: String): Flow<AiGenerationResult> = flow {
        val useLocal = localNanoEngine.checkStatus() == AiAvailability.AVAILABLE
        if (!useLocal && !cloudFallbackEngine.isConfigured()) {
            emit(AiGenerationResult.ModelUnavailable)
            return@flow
        }

        try {
            val engine: AiTextEngine = if (useLocal) localNanoEngine else cloudFallbackEngine
            emit(AiGenerationResult.Success(engine.generateResponse(prompt)))
        } catch (e: Exception) {
            if (useLocal && cloudFallbackEngine.isConfigured()) {
                // The on-device engine reported itself available but failed anyway — try the
                // cloud fallback once before giving up.
                try {
                    emit(AiGenerationResult.Success(cloudFallbackEngine.generateResponse(prompt)))
                } catch (fallbackError: Exception) {
                    emit(AiGenerationResult.Error(fallbackError.message))
                }
            } else {
                emit(AiGenerationResult.Error(e.message))
            }
        }
    }
}
