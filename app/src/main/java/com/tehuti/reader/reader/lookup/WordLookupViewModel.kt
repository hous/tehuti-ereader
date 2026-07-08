package com.tehuti.reader.reader.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.data.ai.AiPrompts
import com.tehuti.reader.domain.model.AiGenerationResult
import com.tehuti.reader.domain.model.LookupResult
import com.tehuti.reader.domain.model.LookupType
import com.tehuti.reader.domain.repo.AiAssistantRepository
import com.tehuti.reader.domain.repo.LookupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LookupUiState {
    data object Loading : LookupUiState
    data class Downloading(val progress: Float?) : LookupUiState
    data class Success(val result: LookupResult) : LookupUiState
    data object NotFound : LookupUiState
    data class Unavailable(val reason: String) : LookupUiState
}

@HiltViewModel
class WordLookupViewModel @Inject constructor(
    private val lookupRepository: LookupRepository,
    private val aiAssistantRepository: AiAssistantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LookupUiState>(LookupUiState.Loading)
    val uiState: StateFlow<LookupUiState> = _uiState

    fun lookup(type: LookupType, word: String, context: String? = null) {
        _uiState.value = LookupUiState.Loading
        viewModelScope.launch {
            when (type) {
                LookupType.DICTIONARY -> {
                    val result = lookupRepository.lookupDictionary(word)
                    _uiState.value = if (result != null) LookupUiState.Success(result) else LookupUiState.NotFound
                }
                LookupType.WIKIPEDIA -> {
                    val result = lookupRepository.lookupWikipedia(word)
                    _uiState.value = if (result != null) LookupUiState.Success(result) else LookupUiState.NotFound
                }
                LookupType.AI_EXPLAIN -> {
                    if (context.isNullOrBlank()) {
                        // Nothing confirmed-read yet to explain from.
                        _uiState.value = LookupUiState.NotFound
                        return@launch
                    }
                    aiAssistantRepository.generate(AiPrompts.explainTermPrompt(word, context))
                        .collect { result ->
                            _uiState.value = when (result) {
                                is AiGenerationResult.Success ->
                                    LookupUiState.Success(LookupResult.AiExplanation(word, result.text))
                                is AiGenerationResult.Downloading -> LookupUiState.Downloading(result.progress)
                                AiGenerationResult.ModelUnavailable ->
                                    LookupUiState.Unavailable("On-device AI isn't available on this device.")
                                is AiGenerationResult.Error -> LookupUiState.NotFound
                            }
                        }
                }
            }
        }
    }
}
