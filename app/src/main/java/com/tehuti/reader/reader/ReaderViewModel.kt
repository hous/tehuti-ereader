package com.tehuti.reader.reader

import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuti.reader.data.books.BookTextExtractor
import com.tehuti.reader.data.books.PublicationFactory
import com.tehuti.reader.data.local.BookDao
import com.tehuti.reader.domain.model.AiAvailability
import com.tehuti.reader.domain.model.LookupType
import com.tehuti.reader.domain.model.ReadingPosition
import com.tehuti.reader.domain.repo.AiAssistantRepository
import com.tehuti.reader.domain.repo.PositionRepository
import com.tehuti.reader.reader.format.ReaderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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

data class LookupRequest(val type: LookupType, val word: String, val context: String? = null)

private const val FORWARD_STREAK_TO_CONFIRM = 4
private const val DELIBERATE_JUMP_THRESHOLD = 0.03
private const val SAME_SPOT_EPSILON = 0.005

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookDao: BookDao,
    private val positionRepository: PositionRepository,
    private val publicationFactory: PublicationFactory,
    private val readerEngine: ReaderEngine,
    private val aiAssistantRepository: AiAssistantRepository,
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])
    private var publication: Publication? = null
    private var positionsCache: List<Locator>? = null
    private var selectableNavigator: SelectableNavigator? = null

    // Resolved once at startup alongside opening the book; the AI menu item/chrome icon are
    // hidden entirely rather than shown-then-erroring when the device can't run on-device AI.
    private var aiExplainAvailable: Boolean = false

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState

    private val _lookupRequest = MutableStateFlow<LookupRequest?>(null)
    val lookupRequest: StateFlow<LookupRequest?> = _lookupRequest

    private val _aiSummaryEnabled = MutableStateFlow(false)
    val aiSummaryEnabled: StateFlow<Boolean> = _aiSummaryEnabled

    private val _summaryRequest = MutableStateFlow<String?>(null)
    val summaryRequest: StateFlow<String?> = _summaryRequest

    // The last position the reader "confirmed" by turning pages forward normally for
    // FORWARD_STREAK_TO_CONFIRM turns in a row. Jumping away from it (backward, or a big
    // forward skip) doesn't move it — it only advances once the user actually keeps reading
    // from the new spot, so a quick skim ahead-and-back doesn't lose the real bookmark.
    private var anchorLocator: Locator? = null
    private var previousProgression: Double? = null
    private var forwardStreak = 0

    // Set whenever the reader jumps away from anchorLocator, so the UI can offer to jump back.
    private val _returnLocator = MutableStateFlow<Locator?>(null)
    val returnLocator: StateFlow<Locator?> = _returnLocator

    // The anchor's progression, surfaced so the progress bar can always show a bookmark
    // for "the confirmed point you've actually read to" — not just when canReturnToPosition.
    private val _bookmarkProgression = MutableStateFlow<Float?>(null)
    val bookmarkProgression: StateFlow<Float?> = _bookmarkProgression

    init {
        viewModelScope.launch {
            val bookDeferred = async { bookDao.getById(bookId) }
            val availabilityDeferred = async { aiAssistantRepository.checkAvailability() }

            val book = bookDeferred.await()
            val pub = book?.let { publicationFactory.open(it.sourceUri.toUri()) }
            if (book == null || pub == null) {
                _uiState.value = ReaderUiState.Error
                return@launch
            }
            publication = pub
            bookDao.updateLastOpenedAt(bookId, System.currentTimeMillis())

            val savedPosition = positionRepository.getPosition(bookId)
            val savedLocator = savedPosition?.locatorJson?.let(::parseLocatorJson)
            // The anchor is the confirmed-reading spoiler boundary — prefer the separately
            // persisted anchor over the raw resume locator, which can be ahead of it (e.g. the
            // user skipped ahead and killed the app before reading any further).
            val savedAnchor = savedPosition?.anchorLocatorJson?.let(::parseLocatorJson) ?: savedLocator
            anchorLocator = savedAnchor
            previousProgression = savedLocator?.locations?.totalProgression
            _bookmarkProgression.value = savedAnchor?.locations?.totalProgression?.toFloat()

            aiExplainAvailable = availabilityDeferred.await() != AiAvailability.UNAVAILABLE
            _aiSummaryEnabled.value = aiExplainAvailable

            _uiState.value = ReaderUiState.Ready(
                fragmentClass = readerEngine.fragmentClass,
                fragmentFactory = readerEngine.createFragmentFactory(
                    pub,
                    savedLocator,
                    ::onSelectionAction,
                    isAiExplainAvailable = { aiExplainAvailable },
                ),
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
            if (word.isNullOrBlank()) return@launch
            // Readium/Publication text handling happens exactly here, before the request is
            // published — WordLookupViewModel downstream never sees a Publication and so can't
            // accidentally read past the spoiler boundary even if it changes later.
            val context = if (type == LookupType.AI_EXPLAIN) {
                publication?.let { pub ->
                    BookTextExtractor.extractReadSoFar(pub, anchorLocator, BookTextExtractor.EXPLAIN_MAX_CHARS)
                } ?: ""
            } else {
                null
            }
            _lookupRequest.value = LookupRequest(type, word, context)
        }
    }

    fun requestSummary() {
        viewModelScope.launch {
            val pub = publication ?: return@launch
            _summaryRequest.value = BookTextExtractor.extractReadSoFar(
                pub,
                anchorLocator,
                BookTextExtractor.SUMMARY_MAX_CHARS,
            )
        }
    }

    fun clearSummaryRequest() {
        _summaryRequest.value = null
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

        if (delta != null && (delta < 0.0 || delta > DELIBERATE_JUMP_THRESHOLD)) {
            // Backward navigation or a deliberate forward jump (progress-bar seek, TOC, etc.):
            // this spot hasn't earned "confirmed reading position" status yet, so offer to
            // return to the last one rather than moving it here immediately.
            forwardStreak = 0
            val confirmed = anchorLocator
            if (confirmed != null && abs(newProgression - (confirmed.locations.totalProgression ?: 0.0)) > SAME_SPOT_EPSILON) {
                _returnLocator.value = confirmed
            }
        } else if (delta != null) {
            // A small forward step, e.g. a single page-turn tap. Only after several of these in
            // a row do we trust that the reader has actually settled here and move the bookmark
            // forward — a couple of quick taps while skimming shouldn't count.
            forwardStreak++
            if (forwardStreak >= FORWARD_STREAK_TO_CONFIRM) {
                setAnchor(locator)
                _returnLocator.value = null
            }
        }

        // The app should always resume exactly where the reader visually left off, independent
        // of the "return to where you left off" bookmark above. The anchor is saved alongside
        // it (not on its own separate write) since setAnchor() above already ran synchronously
        // before this point if it was going to fire for this change.
        viewModelScope.launch {
            positionRepository.savePosition(
                ReadingPosition(
                    bookId = bookId,
                    locatorJson = locator.toJSON().toString(),
                    anchorLocatorJson = anchorLocator?.toJSON()?.toString(),
                    progression = newProgression.toFloat(),
                ),
            )
        }
    }

    fun returnToSavedPosition(): Locator? {
        val locator = _returnLocator.value
        _returnLocator.value = null
        return locator
    }

    private fun setAnchor(locator: Locator) {
        anchorLocator = locator
        _bookmarkProgression.value = locator.locations.totalProgression?.toFloat()
    }

    private fun parseLocatorJson(json: String): Locator? =
        runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()

    override fun onCleared() {
        publication?.close()
    }
}
