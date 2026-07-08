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

sealed interface RecapUiState {
    data object Loading : RecapUiState
    data class Downloading(val progress: Float?) : RecapUiState
    data class Success(val text: String) : RecapUiState
    data object NotEnoughContent : RecapUiState
    data object Unavailable : RecapUiState
    data object Error : RecapUiState
}

@HiltViewModel
class BookSummaryViewModel @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository,
) : ViewModel() {

    // The quick "where you left off" recap loads automatically and is shown by default.
    private val _quickRecapState = MutableStateFlow<RecapUiState>(RecapUiState.Loading)
    val quickRecapState: StateFlow<RecapUiState> = _quickRecapState

    // The full recap only loads once the reader taps "Full recap up to your bookmark" — null
    // means "not requested yet", distinct from any in-progress/finished state.
    private val _fullRecapState = MutableStateFlow<RecapUiState?>(null)
    val fullRecapState: StateFlow<RecapUiState?> = _fullRecapState

    fun loadQuickRecap(excerpt: String) {
        viewModelScope.launch {
            generate(excerpt, AiPrompts::quickRecapPrompt) { _quickRecapState.value = it }
        }
    }

    fun loadFullRecap(excerpt: String) {
        _fullRecapState.value = RecapUiState.Loading
        viewModelScope.launch {
            generate(excerpt, AiPrompts::summaryPrompt) { _fullRecapState.value = it }
        }
    }

    private suspend fun generate(
        excerpt: String,
        prompt: (String) -> String,
        emit: (RecapUiState) -> Unit,
    ) {
        if (excerpt.isBlank()) {
            emit(RecapUiState.NotEnoughContent)
            return
        }
        aiAssistantRepository.generate(prompt(excerpt)).collect { result ->
            emit(
                when (result) {
                    is AiGenerationResult.Success -> RecapUiState.Success(result.text)
                    is AiGenerationResult.Downloading -> RecapUiState.Downloading(result.progress)
                    AiGenerationResult.ModelUnavailable -> RecapUiState.Unavailable
                    is AiGenerationResult.Error -> RecapUiState.Error
                },
            )
        }
    }
}
