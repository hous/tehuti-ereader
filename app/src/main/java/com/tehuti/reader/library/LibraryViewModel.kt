package com.tehuti.reader.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.data.books.BooksDirectoryAccess
import com.tehuti.reader.domain.model.Book
import com.tehuti.reader.domain.model.LibrarySortOrder
import com.tehuti.reader.domain.repo.LibraryRepository
import com.tehuti.reader.domain.repo.SettingsRepository
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
    val isRefreshing: Boolean = false,
    val hasBooksFolder: Boolean = false,
    val books: List<Book> = emptyList(),
    val sortOrder: LibrarySortOrder = LibrarySortOrder.LAST_READ,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val booksDirectoryAccess: BooksDirectoryAccess,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private var currentTreeUri: Uri? = null

    private val booksState: StateFlow<LibraryUiState> = booksDirectoryAccess.booksTreeUri
        .distinctUntilChanged()
        .flatMapLatest { treeUri ->
            currentTreeUri = treeUri
            if (treeUri == null) {
                flowOf(LibraryUiState(hasBooksFolder = false))
            } else {
                isRefreshing.value = true
                viewModelScope.launch { rescan(treeUri) }
                libraryRepository.observeBooks().combine(isRefreshing) { books, refreshing ->
                    LibraryUiState(
                        isRefreshing = refreshing,
                        hasBooksFolder = true,
                        books = books,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    val uiState: StateFlow<LibraryUiState> = booksState
        .combine(settingsRepository.librarySortOrder) { state, sortOrder ->
            state.copy(books = sortBooks(state.books, sortOrder), sortOrder = sortOrder)
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

    fun setSortOrder(order: LibrarySortOrder) {
        viewModelScope.launch { settingsRepository.setLibrarySortOrder(order) }
    }

    private suspend fun rescan(treeUri: Uri) {
        isRefreshing.value = true
        try {
            libraryRepository.rescan(treeUri)
        } finally {
            isRefreshing.value = false
        }
    }

    private fun sortBooks(books: List<Book>, sortOrder: LibrarySortOrder): List<Book> =
        when (sortOrder) {
            LibrarySortOrder.LAST_READ ->
                books.sortedWith(compareByDescending<Book> { it.lastOpenedAt ?: -1L }.thenBy { it.title.lowercase() })
            LibrarySortOrder.TITLE ->
                books.sortedBy { it.title.lowercase() }
            LibrarySortOrder.DATE_ADDED ->
                books.sortedWith(compareByDescending<Book> { it.addedAt }.thenBy { it.title.lowercase() })
        }
}
