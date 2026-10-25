// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.home.ui.authorMetadata
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.ui.editor.BookDetailsDraft
import app.campfire.management.ui.match.FakeMetadataMatchRepository.Companion.Audible
import app.campfire.management.ui.match.FakeMetadataMatchRepository.Companion.Google
import app.campfire.management.ui.match.FakeMetadataMatchRepository.Search
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class MatchPresenterTest {

  @BeforeTest
  fun setUpMain() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDownMain() {
    Dispatchers.resetMain()
  }

  private val details = mediaMetadata(
    title = "Children of Ruin",
    subtitle = null,
    authorName = "Adrian Tchaikovsky",
    ASIN = "B07NDMXD3K",
    authors = listOf(authorMetadata(id = "author-1", name = "Adrian Tchaikovsky")),
  )
  private val book = libraryItem(id = BookId, media = media(metadata = details))
  private val match = BookMatch(
    title = "Children of Ruin",
    subtitle = "Children of Time, Book 2",
    authors = listOf("Adrian Tchaikovsky"),
    genres = listOf("Space Opera"),
    coverUrl = NewCover,
  )

  private val repository = FakeMetadataMatchRepository()
  private var isActive by mutableStateOf(true)
  private val applied = mutableListOf<Applied>()

  private data class Applied(
    val match: BookMatch,
    val fields: Set<MatchField>,
    val merges: Map<MatchField, ListMerge>,
    val providerName: String,
    val save: Boolean,
  )

  @Test
  fun `nothing loads until the section shows`() = runTest {
    isActive = false

    presenter().test {
      assertThat(awaitItem().search).isEqualTo(MatchSearch.NotSearched)
      assertThat(repository.providerLoads).isEqualTo(0)

      isActive = true

      awaitFound()
      assertThat(repository.providerLoads).isEqualTo(1)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `audible is searched first, for the book's ASIN`() = runTest {
    presenter().test {
      val found = awaitFound()

      assertThat(found.provider).isEqualTo(Audible)
      assertThat(found.title.text.toString()).isEqualTo("B07NDMXD3K")
      assertThat(repository.searches).containsExactly(Search(BookId, "audible", "B07NDMXD3K", ""))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the source searched last comes first, for the book's title and author`() = runTest {
    repository.lastProvider = "google"

    presenter().test {
      assertThat(awaitFound().provider).isEqualTo(Google)
      assertThat(repository.searches)
        .containsExactly(Search(BookId, "google", "Children of Ruin", "Adrian Tchaikovsky"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `another source searches again, for what suits it unless something was typed`() = runTest {
    presenter().test {
      val found = awaitFound()

      found.eventSink(MatchUiEvent.ChooseProvider(Google))
      awaitFound(Google)
      assertThat(repository.searches.last())
        .isEqualTo(Search(BookId, "google", "Children of Ruin", "Adrian Tchaikovsky"))

      found.title.setTextAndPlaceCursorAtEnd("Ruin")
      found.eventSink(MatchUiEvent.ChooseProvider(Audible))
      awaitFound(Audible)
      assertThat(repository.searches.last()).isEqualTo(Search(BookId, "audible", "Ruin", "Adrian Tchaikovsky"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a book that looks complete only takes what it lacks at first`() = runTest {
    presenter().test {
      awaitFound().eventSink(MatchUiEvent.Review(match))

      val review = awaitReview()
      assertThat(review.mode).isEqualTo(SelectionMode.EmptyOnly)
      assertThat(review.selected).containsOnly(MatchField.Subtitle)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a book without a cover takes everything at first`() = runTest {
    presenter(item = book.withoutCover()).test {
      awaitFound().eventSink(MatchUiEvent.Review(match))

      val review = awaitReview()
      assertThat(review.mode).isEqualTo(SelectionMode.All)
      assertThat(review.selected).containsOnly(MatchField.Cover, MatchField.Subtitle, MatchField.Genres)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the cover stays out without the upload permission`() = runTest {
    presenter(item = book.withoutCover(), canChangeCover = false).test {
      awaitFound().eventSink(MatchUiEvent.Review(match))
      val state = awaitState { it.review != null }
      assertThat(state.review!!.selected).containsOnly(MatchField.Subtitle, MatchField.Genres)

      state.eventSink(MatchUiEvent.Toggle(MatchField.Cover))
      state.eventSink(MatchUiEvent.Toggle(MatchField.Subtitle))

      assertThat(awaitReview { MatchField.Subtitle !in it.selected }.selected).containsOnly(MatchField.Genres)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `applying hands on the chosen details and closes the review`() = runTest {
    presenter().test {
      awaitFound().eventSink(MatchUiEvent.Review(match))
      val state = awaitState { it.review != null }

      state.eventSink(MatchUiEvent.Toggle(MatchField.Genres))
      state.eventSink(MatchUiEvent.Merge(MatchField.Genres, ListMerge.Add))
      awaitReview { it.merges.isNotEmpty() }
      state.eventSink(MatchUiEvent.ApplyAndSave)

      assertThat(awaitState { it.review == null }.review).isNull()
      assertThat(applied).containsExactly(
        Applied(
          match = match,
          fields = setOf(MatchField.Subtitle, MatchField.Genres),
          merges = mapOf(MatchField.Genres to ListMerge.Add),
          providerName = "Audible.com",
          save = true,
        ),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `nothing chosen applies nothing`() = runTest {
    presenter().test {
      awaitFound().eventSink(MatchUiEvent.Review(match))
      val state = awaitState { it.review != null }

      state.eventSink(MatchUiEvent.Select(SelectionMode.None))
      awaitReview { it.selected.isEmpty() }
      state.eventSink(MatchUiEvent.ApplyToForm)

      assertThat(applied).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a failed search can be tried again`() = runTest {
    repository.searchResult = { Result.failure(IllegalStateException("Timed out")) }

    presenter().test {
      val failed = awaitState { it.search is MatchSearch.Failed }
      assertThat(failed.search).isEqualTo(MatchSearch.Failed(Audible))

      repository.searchResult = { Result.success(listOf(match)) }
      failed.eventSink(MatchUiEvent.Search)

      assertThat((awaitFound().search as MatchSearch.Found).matches).containsExactly(match)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `sources that wouldn't load can be tried again`() = runTest {
    repository.providersResult = Result.failure(IllegalStateException("Offline"))

    presenter().test {
      val failed = awaitState { it.search is MatchSearch.Failed }
      assertThat(failed.search).isEqualTo(MatchSearch.Failed(null))

      repository.providersResult = Result.success(listOf(Audible))
      failed.eventSink(MatchUiEvent.Search)

      awaitFound()
      assertThat(repository.providerLoads).isEqualTo(2)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun presenter(
    item: LibraryItem = book,
    canChangeCover: Boolean = true,
  ): Flow<MatchUiState> {
    val presenter = MatchPresenter(repository)
    val book = item.media as Media.Book
    val draft = BookDetailsDraft(book.metadata, book.tags)
    // Not a screen's presenter, so composed as is rather than through Circuit's
    return moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(
        item = item,
        draft = draft,
        isActive = isActive,
        canChangeCover = canChangeCover,
      ) { match, fields, merges, providerName, save ->
        applied += Applied(match, fields, merges, providerName, save)
      }
    }
  }

  private fun LibraryItem.withoutCover() = copy(media = media(metadata = details, coverPath = null))

  private suspend fun ReceiveTurbine<MatchUiState>.awaitFound(provider: MatchProvider? = null): MatchUiState =
    awaitState { state ->
      val search = state.search as? MatchSearch.Found
      search != null && (provider == null || search.provider == provider)
    }

  private suspend fun ReceiveTurbine<MatchUiState>.awaitReview(
    predicate: (MatchReviewState) -> Boolean = { true },
  ): MatchReviewState = awaitState { state -> state.review?.let(predicate) == true }.review!!

  private suspend fun ReceiveTurbine<MatchUiState>.awaitState(
    predicate: (MatchUiState) -> Boolean,
  ): MatchUiState {
    var state = awaitItem()
    while (!predicate(state)) state = awaitItem()
    return state
  }

  private companion object {
    const val BookId = "book"
    const val NewCover = "https://m.media-amazon.com/images/I/cover.jpg"
  }
}
