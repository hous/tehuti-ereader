package com.tehuti.reader.data.ai

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.tehuti.reader.domain.model.AiAvailability
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Track 1 — the production on-device engine: Gemini Nano via AICore, through the ML Kit GenAI
 * Prompt API (com.google.mlkit:genai-prompt, beta as of 1.0.0-beta2 / genai-common 1.0.0-beta3).
 * The only file that touches that API directly — isolated here so drift in that beta surface is
 * a one-file fix. Verified against the actual compiled AAR, not documentation.
 *
 * Only used when [checkStatus] reports [AiAvailability.AVAILABLE] — see
 * [AiAssistantRepositoryImpl] for the routing logic that falls back to [CloudFallbackEngine]
 * otherwise (unsupported device, emulator, or the model not yet downloaded). Because of that,
 * this class deliberately does not drive AICore's own model-download flow: waiting on a
 * multi-hundred-MB download when a cloud fallback is one request away is worse UX, not better.
 */
@Singleton
class LocalNanoEngine @Inject constructor() : AiTextEngine {

    private val model: GenerativeModel by lazy { Generation.getClient() }

    suspend fun checkStatus(): AiAvailability = try {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> AiAvailability.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> AiAvailability.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> AiAvailability.DOWNLOADING
            else -> AiAvailability.UNAVAILABLE
        }
    } catch (e: Exception) {
        // No AICore on this device, or any other setup failure — treat as unavailable rather
        // than crashing, matching this app's existing graceful-degradation conventions.
        AiAvailability.UNAVAILABLE
    }

    override suspend fun generateResponse(prompt: String): String {
        val response = model.generateContent(prompt)
        return response.candidates.firstOrNull()?.text
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Empty response from the on-device model.")
    }
}
