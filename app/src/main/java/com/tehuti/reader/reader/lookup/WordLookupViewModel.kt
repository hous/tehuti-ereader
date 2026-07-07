package com.tehuti.reader.reader.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.domain.model.LookupResult
import com.tehuti.reader.domain.model.LookupType
import com.tehuti.reader.domain.repo.LookupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LookupUiState {
    data object Loading : LookupUiState
    data class Success(val result: LookupResult) : LookupUiState
    data object NotFound : LookupUiState
}

@HiltViewModel
class WordLookupViewModel @Inject constructor(
    private val lookupRepository: LookupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LookupUiState>(LookupUiState.Loading)
    val uiState: StateFlow<LookupUiState> = _uiState

    fun lookup(type: LookupType, word: String) {
        _uiState.value = LookupUiState.Loading
        viewModelScope.launch {
            val result = when (type) {
                LookupType.DICTIONARY -> lookupRepository.lookupDictionary(word)
                LookupType.WIKIPEDIA -> lookupRepository.lookupWikipedia(word)
            }
            _uiState.value = if (result != null) LookupUiState.Success(result) else LookupUiState.NotFound
        }
    }
}
