package com.tehuti.reader.domain.model

/** Whether on-device generative AI (Gemini Nano via AICore) can be used on this device. */
enum class AiAvailability {
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE,
}

/** Result of a single on-device generation request. */
sealed interface AiGenerationResult {
    data class Success(val text: String) : AiGenerationResult
    data class Downloading(val progress: Float?) : AiGenerationResult
    data object ModelUnavailable : AiGenerationResult
    data class Error(val message: String? = null) : AiGenerationResult
}
