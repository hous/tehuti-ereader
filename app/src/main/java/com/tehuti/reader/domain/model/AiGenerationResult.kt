package com.tehuti.reader.domain.model

/** Whether on-device generative AI (Gemini Nano via AICore) can be used on this device. */
enum class AiAvailability {
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE,
}

/** Result of a single AI generation request (on-device or cloud-fallback — see AiAssistantRepositoryImpl). */
sealed interface AiGenerationResult {
    data class Success(val text: String) : AiGenerationResult

    // Currently unreachable: AiAssistantRepositoryImpl routes to the cloud fallback rather than
    // waiting on an on-device model download. Kept for UI forward-compatibility (WordLookupDialog/
    // BookSummaryDialog already render it) if opportunistic background downloading is added later.
    data class Downloading(val progress: Float?) : AiGenerationResult

    data object ModelUnavailable : AiGenerationResult
    data class Error(val message: String? = null) : AiGenerationResult
}
