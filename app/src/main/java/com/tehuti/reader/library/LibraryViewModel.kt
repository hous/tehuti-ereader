package com.tehuti.reader.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.data.books.BooksDirectoryAccess
import com.tehuti.reader.domain.model.Book
import com.tehuti.reader.domain.repo.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val isLoading: Boolean = true,
    val hasBooksFolder: Boolean = false,
    val books: List<Book> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val booksDirectoryAccess: BooksDirectoryAccess,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = booksDirectoryAccess.booksTreeUri
        .flatMapLatest { treeUri ->
            if (treeUri == null) {
                flowOf(LibraryUiState(isLoading = false, hasBooksFolder = false))
            } else {
                flowOf(treeUri).map { uri ->
                    LibraryUiState(
                        isLoading = false,
                        hasBooksFolder = true,
                        books = libraryRepository.scanBooks(uri),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onFolderPicked(uri: Uri) {
        viewModelScope.launch {
            booksDirectoryAccess.persistTreeUri(uri)
        }
    }
}
