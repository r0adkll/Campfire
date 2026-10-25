// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.cover

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
import app.campfire.libraries.api.CoverUpdate
import app.campfire.management.api.cover.CoverRepository
import app.campfire.management.api.match.CoverSizeReader
import app.campfire.management.api.match.ImageSize
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.api.match.MetadataMatchRepository
import app.campfire.management.ui.editor.BookDetailsDraft
import app.campfire.management.ui.editor.StagedCover
import com.slack.circuit.retained.rememberRetained
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Drives choosing a new cover for a book: searching the server's sources, an image's address, or
 * the book's own images, or none at all. A chosen cover goes in [draft], to take the place of the
 * book's own once the details are saved. Nothing loads until the section is [isActive].
 */
@Inject
class CoverPresenter(
  private val coverRepository: CoverRepository,
  private val matchRepository: MetadataMatchRepository,
  private val coverSizes: CoverSizeReader,
) {

  @Composable
  fun present(
    item: LibraryItem?,
    draft: BookDetailsDraft?,
    isActive: Boolean,
    canUseAddresses: Boolean,
    canRemove: Boolean,
  ): CoverUiState {
    val scope = rememberRetainedCoroutineScope()

    var started by rememberRetained { mutableStateOf(false) }
    var mode by rememberRetained { mutableStateOf(if (canUseAddresses) CoverMode.Search else CoverMode.BookFiles) }
    var providers by rememberRetained { mutableStateOf(emptyList<MatchProvider>()) }
    var source by rememberRetained { mutableStateOf<CoverSource?>(null) }
    val title = rememberRetained { TextFieldState() }
    val author = rememberRetained { TextFieldState() }
    val address = rememberRetained { TextFieldState() }
    var results by rememberRetained { mutableStateOf(emptyList<CoverResults>()) }
    var searchJob by rememberRetained { mutableStateOf<Job?>(null) }
    var bookImages by rememberRetained { mutableStateOf<BookImages>(BookImages.Loading) }
    var preview by rememberRetained { mutableStateOf<CoverPreview?>(null) }
    // Covers' sizes by address, read from their files a few at a time, each once
    var sizes by rememberRetained { mutableStateOf(emptyMap<String, ImageSize>()) }
    val measured = rememberRetained { mutableSetOf<String>() }
    val sizeReads = rememberRetained { Semaphore(ConcurrentSizeReads) }

    val currentUrl = item?.media?.takeIf { it.coverPath != null }?.coverImageUrl

    fun measure(url: String) {
      if (!measured.add(url)) return
      scope.launch {
        sizeReads.withPermit {
          coverSizes.sizeOf(url)
            .onSuccess { sizes = sizes + (url to it) }
            .onFailure { error -> bark(throwable = error) { "Couldn't tell how big the cover at $url is" } }
        }
      }
    }

    fun searched(provider: MatchProvider, search: CoverSearch) {
      results = results.map { if (it.provider == provider) it.copy(search = search) else it }
    }

    fun runSearch(chosen: CoverSource) {
      searchJob?.cancel()
      val searching = when (chosen) {
        CoverSource.Best -> BestSources.mapNotNull { id -> providers.firstOrNull { it.id == id } }
        is CoverSource.Single -> listOf(chosen.provider)
      }
      results = searching.map { CoverResults(it, CoverSearch.Searching) }
      val terms = title.text.toString() to author.text.toString()
      // Each source on its own, so the quick ones show while the slow ones are still looking
      searchJob = scope.launch {
        searching.forEach { provider ->
          launch {
            coverRepository.searchCovers(provider.id, terms.first, terms.second)
              .onSuccess { covers ->
                searched(provider, CoverSearch.Found(covers))
                covers.forEach(::measure)
              }
              .onFailure { error ->
                bark(throwable = error) { "Failed to search ${provider.id} for covers" }
                searched(provider, CoverSearch.Failed)
              }
          }
        }
      }
      Analytics.send(ActionEvent("cover_item", "searched", (chosen as? CoverSource.Single)?.provider?.id ?: "best"))
    }

    fun loadBookImages(itemId: String) {
      bookImages = BookImages.Loading
      scope.launch {
        coverRepository.getBookImages(itemId)
          .onSuccess { bookImages = BookImages.Loaded(it) }
          .onFailure { error ->
            bark(throwable = error) { "Failed to list the images among $itemId's files" }
            bookImages = BookImages.Failed
          }
      }
    }

    // The sources, the book's own images and a first search, once the section first shows
    fun start(book: LibraryItem) {
      book.media.metadata.let { metadata ->
        title.setTextAndPlaceCursorAtEnd(metadata.title.orEmpty())
        author.setTextAndPlaceCursorAtEnd(metadata.authorName.orEmpty())
      }
      currentUrl?.let(::measure)
      loadBookImages(book.id)
      if (!canUseAddresses) return
      scope.launch {
        matchRepository.getBookProviders()
          .onSuccess { listed ->
            providers = listed
            source = CoverSource.Best
            runSearch(CoverSource.Best)
          }
          .onFailure { error ->
            bark(throwable = error) { "Failed to list the sources to find covers in" }
          }
      }
    }

    LaunchedEffect(isActive, item != null) {
      if (isActive && item != null && !started) {
        started = true
        start(item)
      }
    }

    return CoverUiState(
      currentUrl = currentUrl,
      currentSize = currentUrl?.let(sizes::get),
      staged = draft?.newCover,
      mode = mode,
      sources = if (providers.isEmpty()) emptyList() else listOf(CoverSource.Best) + providers.map(CoverSource::Single),
      source = source,
      title = title,
      author = author,
      results = results,
      sizes = sizes,
      address = address,
      bookImages = bookImages,
      canUseAddresses = canUseAddresses,
      canRemove = canRemove,
      preview = preview?.copy(size = preview?.url?.let(sizes::get)),
    ) { event ->
      when (event) {
        is CoverUiEvent.ChooseMode -> {
          if (event.mode != CoverMode.BookFiles && !canUseAddresses) return@CoverUiState
          mode = event.mode
        }

        is CoverUiEvent.ChooseSource -> {
          source = event.source
          runSearch(event.source)
        }

        CoverUiEvent.Search -> {
          val chosen = source
          if (chosen != null) runSearch(chosen) else item?.let(::start)
        }

        is CoverUiEvent.Preview -> {
          // Paging goes through every cover found, in the order they show
          val covers = results.flatMap { (it.search as? CoverSearch.Found)?.covers.orEmpty() }
          val index = covers.indexOf(event.url)
          preview = if (index >= 0) CoverPreview(covers, index) else CoverPreview(listOf(event.url), 0)
          measure(event.url)
        }

        CoverUiEvent.PreviewAddress -> {
          val url = address.text.toString().trim()
          if (!url.isWebAddress()) return@CoverUiState
          preview = CoverPreview(listOf(url), 0)
          measure(url)
        }

        is CoverUiEvent.PageTo -> {
          val current = preview ?: return@CoverUiState
          if (event.index !in current.covers.indices) return@CoverUiState
          preview = current.copy(index = event.index)
          measure(current.covers[event.index])
        }

        CoverUiEvent.ClosePreview -> preview = null

        CoverUiEvent.UsePreviewed -> {
          val chosen = preview ?: return@CoverUiState
          if (!canUseAddresses) return@CoverUiState
          Analytics.send(ActionEvent("cover_item", "chosen", if (mode == CoverMode.Address) "address" else "search"))
          draft?.newCover = StagedCover.fromUrl(chosen.url)
          preview = null
        }

        is CoverUiEvent.UseBookImage -> {
          Analytics.send(ActionEvent("cover_item", "chosen", "file"))
          draft?.newCover = if (event.image.isCover) {
            null
          } else {
            StagedCover(CoverUpdate.FromServerFile(event.image.path), event.image.url, event.image.name)
          }
        }

        CoverUiEvent.RetryBookImages -> item?.id?.let(::loadBookImages)

        CoverUiEvent.Remove -> {
          if (!canRemove || currentUrl == null) return@CoverUiState
          Analytics.send(ActionEvent("cover_item", "chosen", "remove"))
          draft?.newCover = StagedCover.Removed
        }

        CoverUiEvent.KeepCurrent -> draft?.newCover = null
      }
    }
  }
}

private fun String.isWebAddress(): Boolean = startsWith("https://") || startsWith("http://")

/** The sources the server's own "best" cover search asks */
private val BestSources = listOf("audible", "google", "fantlab")

private const val ConcurrentSizeReads = 4
