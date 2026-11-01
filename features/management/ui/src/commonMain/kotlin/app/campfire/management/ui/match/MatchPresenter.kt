// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.CoverSizeReader
import app.campfire.management.api.match.ImageSize
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.api.match.MetadataMatchRepository
import app.campfire.management.ui.editor.BookDetailsDraft
import com.slack.circuit.retained.rememberRetained
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** What to do with a result's chosen details */
fun interface MatchApplier {
  fun apply(
    match: BookMatch,
    fields: Set<MatchField>,
    merges: Map<MatchField, ListMerge>,
    providerName: String,
    save: Boolean,
  )
}

/**
 * Drives matching a book against a source: listing the sources, searching one, and comparing a
 * result with what's in [draft], to [MatchApplier.apply] the details chosen from it. Nothing
 * loads until the section is [isActive].
 */
@Inject
class MatchPresenter(
  private val repository: MetadataMatchRepository,
  private val coverSizes: CoverSizeReader,
) {

  @Composable
  fun present(
    item: LibraryItem?,
    draft: BookDetailsDraft?,
    isActive: Boolean,
    canChangeCover: Boolean,
    applier: MatchApplier,
  ): MatchUiState {
    val scope = rememberRetainedCoroutineScope()

    var providers by rememberRetained { mutableStateOf(emptyList<MatchProvider>()) }
    var providerId by rememberRetained { mutableStateOf<String?>(null) }
    val title = rememberRetained { TextFieldState() }
    val author = rememberRetained { TextFieldState() }
    var search by rememberRetained { mutableStateOf<MatchSearch>(MatchSearch.NotSearched) }
    var searchJob by rememberRetained { mutableStateOf<Job?>(null) }
    // The search as it was filled in for the book, before any typing
    var prefilled by rememberRetained { mutableStateOf<Pair<String, String>?>(null) }

    var reviewing by rememberRetained { mutableStateOf<BookMatch?>(null) }
    var selected by rememberRetained { mutableStateOf(emptySet<MatchField>()) }
    var merges by rememberRetained { mutableStateOf(emptyMap<MatchField, ListMerge>()) }
    // Covers' sizes by address, read from their files as reviews show them
    var sizes by rememberRetained { mutableStateOf(emptyMap<String, ImageSize>()) }

    val provider = providers.firstOrNull { it.id == providerId }
    val coverUrl = item?.media?.takeIf { it.coverPath != null }?.coverImageUrl

    // Events read the state as it is, not as it was when the state they came from was made
    fun currentProvider() = providers.firstOrNull { it.id == providerId }
    fun currentRows() = reviewing?.let { match -> draft?.let { reviewRows(match, it, coverUrl) } }
    fun providerName() = (search as? MatchSearch.Found)?.provider?.name ?: currentProvider()?.name.orEmpty()

    // Audible finds a book by its ASIN best, as the server's own matching does; the rest by its
    // title and author
    fun prefill(source: MatchProvider?) {
      val metadata = item?.media?.metadata ?: return
      val asin = metadata.ASIN?.takeIf { source?.id?.startsWith(AudibleProvider) == true }
      val query = if (asin != null) asin to "" else metadata.title.orEmpty() to metadata.authorName.orEmpty()
      title.setTextAndPlaceCursorAtEnd(query.first)
      author.setTextAndPlaceCursorAtEnd(query.second)
      prefilled = query
    }

    fun isPrefilled() = prefilled == null || prefilled == (title.text.toString() to author.text.toString())

    fun measure(url: String) {
      if (url in sizes) return
      scope.launch {
        coverSizes.sizeOf(url)
          .onSuccess { sizes = sizes + (url to it) }
          .onFailure { error -> bark(throwable = error) { "Couldn't tell how big the cover at $url is" } }
      }
    }

    fun runSearch(source: MatchProvider) {
      val itemId = item?.id ?: return
      searchJob?.cancel()
      reviewing = null
      search = MatchSearch.Searching
      searchJob = scope.launch {
        repository.searchBooks(itemId, source.id, title.text.toString(), author.text.toString())
          .onSuccess { matches ->
            Analytics.send(ActionEvent("match_item", "searched", source.id))
            search = MatchSearch.Found(source, matches)
          }
          .onFailure { error ->
            bark(throwable = error) { "Failed to search ${source.id} for $itemId" }
            search = MatchSearch.Failed(source)
          }
      }
    }

    // The sources, then the first search, once the section first shows
    fun start() {
      if (item == null) return
      searchJob?.cancel()
      search = MatchSearch.Searching
      searchJob = scope.launch {
        repository.getBookProviders()
          .onSuccess { listed ->
            providers = listed
            val source = listed.firstOrNull { it.id == repository.lastProvider }
              ?: listed.firstOrNull { it.id == DefaultProvider }
              ?: listed.firstOrNull()
            providerId = source?.id
            if (isPrefilled()) prefill(source)
            if (source != null) runSearch(source) else search = MatchSearch.Failed(null)
          }
          .onFailure { error ->
            bark(throwable = error) { "Failed to list the sources to match ${item.id} from" }
            search = MatchSearch.Failed(null)
          }
      }
    }

    LaunchedEffect(isActive, item != null) {
      if (isActive && item != null && providers.isEmpty() && search == MatchSearch.NotSearched) start()
    }

    val review = reviewing?.let { match ->
      currentRows()?.let {
        MatchReviewState(
          match = match,
          providerName = providerName(),
          rows = it,
          selected = selected,
          merges = merges,
          canChangeCover = canChangeCover,
          currentCoverSize = coverUrl?.let(sizes::get),
          newCoverSize = match.coverUrl?.let(sizes::get),
        )
      }
    }

    return MatchUiState(
      providers = providers,
      provider = provider,
      title = title,
      author = author,
      search = search,
      bookDuration = item?.media?.duration?.takeIf { it.isPositive() },
      bookAsin = item?.media?.metadata?.ASIN,
      review = review,
      canChangeCover = canChangeCover,
    ) { event ->
      when (event) {
        is MatchUiEvent.ChooseProvider -> {
          providerId = event.provider.id
          // What was filled in suits the new source instead, while what was typed stays
          if (isPrefilled()) prefill(event.provider)
          if (search !is MatchSearch.NotSearched) runSearch(event.provider)
        }

        MatchUiEvent.Search -> {
          val source = currentProvider()
          if (source == null) start() else runSearch(source)
        }

        is MatchUiEvent.Review -> {
          val current = draft ?: return@MatchUiState
          val opened = reviewRows(event.match, current, coverUrl)
          // Never overwrite a book that already looks complete without being asked
          val looksComplete = current.title.text.isNotBlank() && current.authors.values.isNotEmpty() && coverUrl != null
          selected = opened.selectedBy(
            if (looksComplete) SelectionMode.EmptyOnly else SelectionMode.All,
            canChangeCover,
          )
          merges = emptyMap()
          // Last, so the review shows with its choices already made
          reviewing = event.match
          event.match.coverUrl?.let { new ->
            measure(new)
            coverUrl?.let(::measure)
          }
        }

        MatchUiEvent.CloseReview -> reviewing = null

        is MatchUiEvent.Select -> selected = currentRows().orEmpty().selectedBy(event.mode, canChangeCover)

        is MatchUiEvent.Toggle -> {
          if (event.field == MatchField.Cover && !canChangeCover) return@MatchUiState
          selected = if (event.field in selected) selected - event.field else selected + event.field
        }

        is MatchUiEvent.Merge -> merges = merges + (event.field to event.merge)

        MatchUiEvent.ApplyToForm, MatchUiEvent.ApplyAndSave -> {
          val match = reviewing ?: return@MatchUiState
          if (selected.isEmpty()) return@MatchUiState
          val save = event == MatchUiEvent.ApplyAndSave
          Analytics.send(ActionEvent("match_item", if (save) "applied_saved" else "applied", selected.size.toString()))
          applier.apply(match, selected, merges, providerName(), save)
          reviewing = null
        }
      }
    }
  }
}

private const val AudibleProvider = "audible"
private const val DefaultProvider = AudibleProvider
