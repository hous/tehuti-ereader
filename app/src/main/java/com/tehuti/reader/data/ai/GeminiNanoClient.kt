package com.tehuti.reader.data.ai

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.tehuti.reader.domain.model.AiAvailability
import com.tehuti.reader.domain.model.AiGenerationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only file in this app that touches the ML Kit GenAI Prompt API (com.google.mlkit:genai-prompt,
 * beta as of 1.0.0-beta2 / genai-common 1.0.0-beta3) directly — isolated here so drift in that
 * beta surface is a one-file fix. Verified against the actual compiled AAR, not documentation.
 */
@Singleton
class GeminiNanoClient @Inject constructor() {

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

    fun generateContent(prompt: String): Flow<AiGenerationResult> = flow {
        val status = checkStatus()
        if (status == AiAvailability.UNAVAILABLE) {
            emit(AiGenerationResult.ModelUnavailable)
            return@flow
        }

        if (status != AiAvailability.AVAILABLE) {
            var bytesToDownload = 0L
            var downloadFailed = false
            model.download().collect { downloadStatus ->
                when (downloadStatus) {
                    is DownloadStatus.DownloadStarted -> bytesToDownload = downloadStatus.bytesToDownload
                    is DownloadStatus.DownloadProgress -> {
                        val fraction = if (bytesToDownload > 0) {
                            (downloadStatus.totalBytesDownloaded.toFloat() / bytesToDownload).coerceIn(0f, 1f)
                        } else {
                            null
                        }
                        emit(AiGenerationResult.Downloading(fraction))
                    }
                    is DownloadStatus.DownloadFailed -> downloadFailed = true
                    else -> Unit
                }
            }
            if (downloadFailed) {
                emit(AiGenerationResult.Error("Couldn't download the on-device model."))
                return@flow
            }
        }

        try {
            val response = model.generateContent(prompt)
            val text = response.candidates.firstOrNull()?.text
            if (text.isNullOrBlank()) {
                emit(AiGenerationResult.Error())
            } else {
                emit(AiGenerationResult.Success(text))
            }
        } catch (e: GenAiException) {
            emit(AiGenerationResult.Error(e.message))
        } catch (e: Exception) {
            emit(AiGenerationResult.Error(e.message))
        }
    }
}
