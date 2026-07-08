package com.tehuti.reader.reader.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.data.ai.AiPrompts
import com.tehuti.reader.domain.model.AiGenerationResult
import com.tehuti.reader.domain.repo.AiAssistantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SummaryUiState {
    data object Loading : SummaryUiState
    data class Downloading(val progress: Float?) : SummaryUiState
    data class Success(val text: String) : SummaryUiState
    data object NotEnoughContent : SummaryUiState
    data object Unavailable : SummaryUiState
    data object Error : SummaryUiState
}

@HiltViewModel
class BookSummaryViewModel @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
    val uiState: StateFlow<SummaryUiState> = _uiState

    fun summarize(excerpt: String) {
        _uiState.value = SummaryUiState.Loading
        viewModelScope.launch {
            if (excerpt.isBlank()) {
                _uiState.value = SummaryUiState.NotEnoughContent
                return@launch
            }
            aiAssistantRepository.generate(AiPrompts.summaryPrompt(excerpt)).collect { result ->
                _uiState.value = when (result) {
                    is AiGenerationResult.Success -> SummaryUiState.Success(result.text)
                    is AiGenerationResult.Downloading -> SummaryUiState.Downloading(result.progress)
                    AiGenerationResult.ModelUnavailable -> SummaryUiState.Unavailable
                    is AiGenerationResult.Error -> SummaryUiState.Error
                }
            }
        }
    }
}
