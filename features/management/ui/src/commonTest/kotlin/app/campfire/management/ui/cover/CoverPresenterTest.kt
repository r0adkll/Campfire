// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.cover

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.campfire.core.model.Media
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.libraries.api.CoverUpdate
import app.campfire.management.api.match.ImageSize
import app.campfire.management.ui.editor.BookDetailsDraft
import app.campfire.management.ui.editor.StagedCover
import app.campfire.management.ui.match.FakeCoverSizeReader
import app.campfire.management.ui.match.FakeMetadataMatchRepository
import app.campfire.management.ui.match.FakeMetadataMatchRepository.Companion.Audible
import app.campfire.management.ui.match.FakeMetadataMatchRepository.Companion.Google
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CoverPresenterTest {

  @BeforeTest
  fun setUpMain() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDownMain() {
    Dispatchers.resetMain()
  }

  private val book = libraryItem(
    id = "book",
    media = media(
      metadata = mediaMetadata(title = "Children of Ruin", authorName = "Adrian Tchaikovsky"),
      coverImageUrl = CurrentCover,
    ),
  )
  private val draft = (book.media as Media.Book).let { BookDetailsDraft(it.metadata, it.tags) }

  private val covers = FakeCoverRepository()
  private val sources = FakeMetadataMatchRepository()
  private val coverSizes = FakeCoverSizeReader()
  private var isActive by mutableStateOf(true)

  @Test
  fun `nothing loads until the section shows`() = runTest {
    isActive = false

    presenter().test {
      assertThat(awaitItem().results).isEmpty()
      assertThat(covers.searches).isEmpty()
      assertThat(covers.bookImageLoads).isEqualTo(0)

      isActive = true

      awaitState { state -> state.results.all { it.search is CoverSearch.Found } && state.results.isNotEmpty() }
      assertThat(covers.bookImageLoads).isEqualTo(1)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the best results search each best source the server lists, by the book's title and author`() = runTest {
    covers.covers["audible"] = Result.success(listOf(AudibleCover))
    covers.covers["google"] = Result.success(listOf(GoogleCover))

    presenter().test {
      val found = awaitAllFound()

      assertThat(found.source).isEqualTo(CoverSource.Best)
      assertThat(found.results).containsExactly(
        CoverResults(Audible, CoverSearch.Found(listOf(AudibleCover))),
        CoverResults(Google, CoverSearch.Found(listOf(GoogleCover))),
      )
      // FantLab's among the best, but this server doesn't list it
      assertThat(covers.searches).containsExactlyInAnyOrder(
        FakeCoverRepository.Search("audible", "Children of Ruin", "Adrian Tchaikovsky"),
        FakeCoverRepository.Search("google", "Children of Ruin", "Adrian Tchaikovsky"),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a quick source shows while a slow one is still looking`() = runTest {
    covers.covers["audible"] = Result.success(listOf(AudibleCover))
    val google = CompletableDeferred<Unit>()
    covers.gates["google"] = google

    presenter().test {
      val partly = awaitState { state -> state.results.any { it.search is CoverSearch.Found } }
      assertThat(partly.results.map { it.search }).containsExactly(
        CoverSearch.Found(listOf(AudibleCover)),
        CoverSearch.Searching,
      )

      google.complete(Unit)

      awaitAllFound()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a source that fails says so beside the others`() = runTest {
    covers.covers["google"] = Result.failure(IllegalStateException("timed out"))

    presenter().test {
      val searched =
        awaitState { state -> state.results.isNotEmpty() && state.results.none { it.search == CoverSearch.Searching } }

      assertThat(searched.results.map { it.search }).containsExactly(CoverSearch.Found(emptyList()), CoverSearch.Failed)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `each cover found has its size read once, with the book's own`() = runTest {
    covers.covers["audible"] = Result.success(listOf(AudibleCover, SharedCover))
    covers.covers["google"] = Result.success(listOf(SharedCover))
    coverSizes.sizes[AudibleCover] = ImageSize(2400, 2400)
    coverSizes.sizes[CurrentCover] = ImageSize(640, 640)

    presenter().test {
      val sized = awaitState { it.sizes.size == 2 && it.currentSize != null }

      assertThat(sized.sizes[AudibleCover]).isEqualTo(ImageSize(2400, 2400))
      assertThat(sized.currentSize).isEqualTo(ImageSize(640, 640))
      assertThat(coverSizes.reads.sorted()).containsExactly(
        *listOf(AudibleCover, CurrentCover, SharedCover).sorted().toTypedArray(),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `one source searches only it, for what was typed`() = runTest {
    presenter().test {
      val found = awaitAllFound()
      covers.searches.clear()

      found.title.setTextAndPlaceCursorAtEnd("Ruin")
      found.eventSink(CoverUiEvent.ChooseSource(CoverSource.Single(Google)))

      val single = awaitState { it.source == CoverSource.Single(Google) && it.results.size == 1 }
      assertThat(single.results.single().provider).isEqualTo(Google)
      assertThat(covers.searches).containsExactly(FakeCoverRepository.Search("google", "Ruin", "Adrian Tchaikovsky"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a cover looked at pages through every one found, and can be chosen`() = runTest {
    covers.covers["audible"] = Result.success(listOf(AudibleCover))
    covers.covers["google"] = Result.success(listOf(GoogleCover))

    presenter().test {
      awaitAllFound().eventSink(CoverUiEvent.Preview(AudibleCover))

      val looking = awaitState { it.preview != null }
      assertThat(looking.preview?.covers).isEqualTo(listOf(AudibleCover, GoogleCover))

      looking.eventSink(CoverUiEvent.PageTo(1))
      val paged = awaitState { it.preview?.index == 1 }
      paged.eventSink(CoverUiEvent.UsePreviewed)

      val chosen = awaitState { it.preview == null }
      assertThat(chosen.staged).isEqualTo(StagedCover.fromUrl(GoogleCover))
      assertThat(draft.newCover).isEqualTo(StagedCover.fromUrl(GoogleCover))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `only something that looks like an address is looked at`() = runTest {
    presenter().test {
      val state = awaitAllFound()
      state.eventSink(CoverUiEvent.ChooseMode(CoverMode.Address))

      state.address.setTextAndPlaceCursorAtEnd("children of ruin")
      state.eventSink(CoverUiEvent.PreviewAddress)
      state.address.setTextAndPlaceCursorAtEnd(" https://example.com/ruin.jpg ")
      state.eventSink(CoverUiEvent.PreviewAddress)

      val looking = awaitState { it.preview != null }
      assertThat(looking.preview?.covers).isEqualTo(listOf("https://example.com/ruin.jpg"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `one of the book's images is chosen by its file, and the one in use keeps the cover`() = runTest {
    val folder = bookImage("folder.jpg")
    val inUse = bookImage("cover.jpg", isCover = true)
    covers.bookImagesResult = Result.success(listOf(inUse, folder))

    presenter().test {
      val loaded = awaitState { it.bookImages is BookImages.Loaded }
      loaded.eventSink(CoverUiEvent.UseBookImage(folder))

      val chosen = awaitState { it.staged != null }
      assertThat(chosen.staged).isEqualTo(
        StagedCover(CoverUpdate.FromServerFile(folder.path), folder.url, "folder.jpg"),
      )

      chosen.eventSink(CoverUiEvent.UseBookImage(inUse))

      assertThat(awaitState { it.staged == null }.staged).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the book's images can be listed again after failing`() = runTest {
    covers.bookImagesResult = Result.failure(IllegalStateException("offline"))

    presenter().test {
      val failed = awaitState { it.bookImages == BookImages.Failed }
      covers.bookImagesResult = Result.success(listOf(bookImage()))
      failed.eventSink(CoverUiEvent.RetryBookImages)

      awaitState { it.bookImages == BookImages.Loaded(listOf(bookImage())) }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `removing chooses no cover, for an account that may, and keeping undoes it`() = runTest {
    presenter(canRemove = false).test {
      awaitAllFound().eventSink(CoverUiEvent.Remove)
      assertThat(draft.newCover).isNull()
      cancelAndIgnoreRemainingEvents()
    }

    presenter(canRemove = true).test {
      awaitAllFound().eventSink(CoverUiEvent.Remove)
      val removing = awaitState { it.staged != null }
      assertThat(removing.staged).isEqualTo(StagedCover.Removed)

      removing.eventSink(CoverUiEvent.KeepCurrent)
      assertThat(awaitState { it.staged == null }.staged).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `without the upload permission nothing's searched, and the book's own images show`() = runTest {
    presenter(canUseAddresses = false).test {
      val state = awaitState { it.bookImages is BookImages.Loaded }
      assertThat(state.mode).isEqualTo(CoverMode.BookFiles)

      state.eventSink(CoverUiEvent.ChooseMode(CoverMode.Search))

      // Still on the book's files
      expectNoEvents()
      assertThat(covers.searches).isEmpty()
      assertThat(sources.providerLoads).isEqualTo(0)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun presenter(
    canUseAddresses: Boolean = true,
    canRemove: Boolean = true,
  ): Flow<CoverUiState> {
    val presenter = CoverPresenter(covers, sources, coverSizes)
    // Not a screen's presenter, so composed as is rather than through Circuit's
    return moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(
        item = book,
        draft = draft,
        isActive = isActive,
        canUseAddresses = canUseAddresses,
        canRemove = canRemove,
      )
    }
  }

  private suspend fun ReceiveTurbine<CoverUiState>.awaitAllFound(): CoverUiState =
    awaitState { state -> state.results.isNotEmpty() && state.results.all { it.search is CoverSearch.Found } }

  private suspend fun ReceiveTurbine<CoverUiState>.awaitState(predicate: (CoverUiState) -> Boolean): CoverUiState {
    var state = awaitItem()
    while (!predicate(state)) state = awaitItem()
    return state
  }

  private companion object {
    const val CurrentCover = "https://abs.example/api/items/book/cover"
    const val AudibleCover = "https://m.media-amazon.com/images/I/ruin.jpg"
    const val GoogleCover = "https://books.google.com/books/content?id=ruin"
    const val SharedCover = "https://covers.openlibrary.org/b/id/ruin.jpg"
  }
}
