package com.tehuti.reader.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.data.books.BooksDirectoryAccess
import com.tehuti.reader.domain.model.Book
import com.tehuti.reader.domain.repo.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasBooksFolder: Boolean = false,
    val books: List<Book> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val booksDirectoryAccess: BooksDirectoryAccess,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private var currentTreeUri: Uri? = null

    val uiState: StateFlow<LibraryUiState> = booksDirectoryAccess.booksTreeUri
        .distinctUntilChanged()
        .flatMapLatest { treeUri ->
            currentTreeUri = treeUri
            if (treeUri == null) {
                flowOf(LibraryUiState(isLoading = false, hasBooksFolder = false))
            } else {
                viewModelScope.launch { rescan(treeUri) }
                libraryRepository.observeBooks().combine(isRefreshing) { books, refreshing ->
                    LibraryUiState(
                        isLoading = false,
                        isRefreshing = refreshing,
                        hasBooksFolder = true,
                        books = books,
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

    fun refresh() {
        val treeUri = currentTreeUri ?: return
        viewModelScope.launch { rescan(treeUri) }
    }

    private suspend fun rescan(treeUri: Uri) {
        isRefreshing.value = true
        try {
            libraryRepository.rescan(treeUri)
        } finally {
            isRefreshing.value = false
        }
    }
}
