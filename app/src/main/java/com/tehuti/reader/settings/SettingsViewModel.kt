package com.tehuti.reader.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.data.books.BooksDirectoryAccess
import com.tehuti.reader.domain.model.AppFontFamily
import com.tehuti.reader.domain.model.AppTheme
import com.tehuti.reader.domain.model.ReaderSettings
import com.tehuti.reader.domain.repo.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val booksDirectoryAccess: BooksDirectoryAccess,
) : ViewModel() {

    val settings: StateFlow<ReaderSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderSettings())

    val booksFolderName: StateFlow<String?> = booksDirectoryAccess.booksTreeUri
        .map { uri -> uri?.let(booksDirectoryAccess::displayNameFor) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFontSizePercent(percent: Int) {
        viewModelScope.launch { settingsRepository.setFontSizePercent(percent) }
    }

    fun setFontFamily(fontFamily: AppFontFamily) {
        viewModelScope.launch { settingsRepository.setFontFamily(fontFamily) }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun onBooksFolderPicked(uri: Uri) {
        viewModelScope.launch { booksDirectoryAccess.persistTreeUri(uri) }
    }
}
