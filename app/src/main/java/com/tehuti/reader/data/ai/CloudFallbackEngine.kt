package com.tehuti.reader.data.ai

import com.tehuti.reader.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Track 2 — the cloud fallback engine: a hosted Gemini model, used whenever [LocalNanoEngine]
 * isn't immediately available (emulator, unsupported device, or the on-device model not yet
 * downloaded). The API key and model name come from BuildConfig, which is populated from a
 * gitignored local `.env` file at build time (see `.env.default` for the template and
 * app/build.gradle.kts for the loading logic) — never hardcoded, never committed.
 */
@Singleton
class CloudFallbackEngine @Inject constructor(
    private val geminiCloudApi: GeminiCloudApi,
) : AiTextEngine {

    fun isConfigured(): Boolean = BuildConfig.CLOUD_AI_API_KEY.isNotBlank()

    override suspend fun generateResponse(prompt: String): String {
        check(isConfigured()) { "No cloud AI API key configured — see .env.default." }
        val response = geminiCloudApi.generateContent(
            model = BuildConfig.CLOUD_AI_MODEL,
            apiKey = BuildConfig.CLOUD_AI_API_KEY,
            request = GeminiGenerateContentRequest(
                contents = listOf(GeminiContentDto(parts = listOf(GeminiPartDto(prompt)))),
            ),
        )
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Empty response from the cloud model.")
    }
}
