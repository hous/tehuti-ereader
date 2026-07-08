package com.tehuti.reader.reader

import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.data.books.PublicationFactory
import com.tehuti.reader.data.local.BookDao
import com.tehuti.reader.domain.model.LookupType
import com.tehuti.reader.domain.model.ReadingPosition
import com.tehuti.reader.domain.repo.PositionRepository
import com.tehuti.reader.reader.format.ReaderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.SelectableNavigator
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

data class LookupRequest(val type: LookupType, val word: String)

private const val FORWARD_STREAK_BEFORE_RETURN = 3
private const val DELIBERATE_JUMP_THRESHOLD = 0.03

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
    private var selectableNavigator: SelectableNavigator? = null

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState

    private val _lookupRequest = MutableStateFlow<LookupRequest?>(null)
    val lookupRequest: StateFlow<LookupRequest?> = _lookupRequest

    // Tracks the reader's "real" position separately from wherever the user has tapped ahead to,
    // so casually browsing forward doesn't silently overwrite the bookmark they'll want back.
    private var anchorLocator: Locator? = null
    private var previousProgression: Double? = null
    private var forwardStreak = 0

    private val _returnLocator = MutableStateFlow<Locator?>(null)
    val returnLocator: StateFlow<Locator?> = _returnLocator

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
            anchorLocator = savedLocator
            previousProgression = savedLocator?.locations?.totalProgression

            _uiState.value = ReaderUiState.Ready(
                fragmentClass = readerEngine.fragmentClass,
                fragmentFactory = readerEngine.createFragmentFactory(pub, savedLocator, ::onSelectionAction),
                title = pub.metadata.title ?: book.title,
            )
        }
    }

    fun attachNavigator(navigator: SelectableNavigator?) {
        selectableNavigator = navigator
    }

    fun clearLookupRequest() {
        _lookupRequest.value = null
    }

    private fun onSelectionAction(type: LookupType) {
        viewModelScope.launch {
            val word = selectableNavigator?.currentSelection()?.locator?.text?.highlight?.trim()
            if (!word.isNullOrBlank()) {
                _lookupRequest.value = LookupRequest(type, word)
            }
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
        val newProgression = locator.locations.totalProgression ?: 0.0
        val delta = previousProgression?.let { newProgression - it }
        previousProgression = newProgression

        // Readium's navigator sometimes re-emits the exact same locator (e.g. on settle after
        // a page turn) with no actual movement — that's not a page turn either way, so leave
        // the streak/anchor untouched rather than treating it as "moved backward."
        if (delta == 0.0) return

        if (delta == null || delta < 0.0 || delta > DELIBERATE_JUMP_THRESHOLD) {
            // First position, backward navigation, or a deliberate jump (e.g. dragging the
            // progress bar): treat wherever the reader landed as their real position.
            forwardStreak = 0
            anchorLocator = locator
            _returnLocator.value = null
        } else {
            // A small forward step, e.g. a single page-turn tap.
            forwardStreak++
            if (forwardStreak <= FORWARD_STREAK_BEFORE_RETURN - 1) {
                anchorLocator = locator
            } else {
                _returnLocator.value = anchorLocator
            }
        }

        val toSave = anchorLocator ?: locator
        viewModelScope.launch {
            positionRepository.savePosition(
                ReadingPosition(
                    bookId = bookId,
                    locatorJson = toSave.toJSON().toString(),
                    progression = toSave.locations.totalProgression?.toFloat() ?: 0f,
                ),
            )
        }
    }

    fun returnToSavedPosition(): Locator? {
        val locator = _returnLocator.value
        _returnLocator.value = null
        return locator
    }

    override fun onCleared() {
        publication?.close()
    }
}
