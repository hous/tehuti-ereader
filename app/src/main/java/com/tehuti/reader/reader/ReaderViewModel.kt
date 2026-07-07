package com.tehuti.reader.reader

import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.data.books.PublicationFactory
import com.tehuti.reader.data.local.BookDao
import com.tehuti.reader.domain.model.ReadingPosition
import com.tehuti.reader.domain.repo.PositionRepository
import com.tehuti.reader.reader.format.ReaderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import javax.inject.Inject
import kotlin.math.abs

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Ready(
        val fragmentClass: Class<out Fragment>,
        val fragmentFactory: FragmentFactory,
        val title: String,
    ) : ReaderUiState
    data object Error : ReaderUiState
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookDao: BookDao,
    private val positionRepository: PositionRepository,
    private val publicationFactory: PublicationFactory,
    private val readerEngine: ReaderEngine,
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])
    private var publication: Publication? = null
    private var positionsCache: List<Locator>? = null

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState

    init {
        viewModelScope.launch {
            val book = bookDao.getById(bookId)
            val pub = book?.let { publicationFactory.open(it.sourceUri.toUri()) }
            if (book == null || pub == null) {
                _uiState.value = ReaderUiState.Error
                return@launch
            }
            publication = pub
            bookDao.updateLastOpenedAt(bookId, System.currentTimeMillis())

            val savedLocator = positionRepository.getPosition(bookId)
                ?.let { runCatching { Locator.fromJSON(JSONObject(it.locatorJson)) }.getOrNull() }

            _uiState.value = ReaderUiState.Ready(
                fragmentClass = readerEngine.fragmentClass,
                fragmentFactory = readerEngine.createFragmentFactory(pub, savedLocator),
                title = pub.metadata.title ?: book.title,
            )
        }
    }

    suspend fun findLocatorForProgression(fraction: Float): Locator? {
        val pub = publication ?: return null
        val positions = positionsCache ?: pub.positions().also { positionsCache = it }
        return positions.minByOrNull { locator ->
            abs((locator.locations.totalProgression ?: 0.0) - fraction)
        }
    }

    fun onLocatorChanged(locator: Locator) {
        viewModelScope.launch {
            positionRepository.savePosition(
                ReadingPosition(
                    bookId = bookId,
                    locatorJson = locator.toJSON().toString(),
                    progression = locator.locations.totalProgression?.toFloat() ?: 0f,
                ),
            )
        }
    }

    override fun onCleared() {
        publication?.close()
    }
}
