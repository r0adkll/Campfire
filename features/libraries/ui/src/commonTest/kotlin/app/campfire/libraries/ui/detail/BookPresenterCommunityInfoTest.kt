// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.api.BookReview
import app.campfire.bookinfo.api.CommunityContent
import app.campfire.bookinfo.api.CommunityInfoState
import app.campfire.bookinfo.api.ProviderId
import app.campfire.common.screens.ConnectedProvidersScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.common.test.assert.containsInstance
import app.campfire.common.test.assert.doesNotContainInstance
import app.campfire.core.coroutines.LoadState
import app.campfire.libraries.ui.detail.composables.slots.CommunitySlot
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.cash.turbine.ReceiveTurbine
import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class BookPresenterCommunityInfoTest : BaseLibraryItemPresenterTest() {

  private val communityInfo = BookCommunityInfo(
    providerBookId = "386446",
    providerUrl = "https://hardcover.app/books/the-way-of-kings",
    rating = 4.63,
    ratingsCount = 4109,
    ratingsDistribution = null,
    reviewsCount = 422,
    releaseDate = "2010-08-31",
    coverUrl = null,
  )

  private fun communityState(
    content: CommunityContent = CommunityContent.Available(communityInfo, emptyList()),
  ) = CommunityInfoState(
    providerId = ProviderId.Hardcover,
    providerName = "Hardcover",
    content = content,
  )

  @Test
  fun present_CommunityInfo_GeneratesCommunitySlot() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    bookInfoRegistry.communityInfoFlow.emit(communityState())

    presenter.test {
      assertThat(awaitItemWithSlot<CommunitySlot>())
        .loadedSlots
        .containsInstance<CommunitySlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_LoadingContent_StillGeneratesCommunitySlot() = runTest {
    // The section renders through the fetch — its loading phase lives inside
    // the slot instead of removing it, so source switches never blink.
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    bookInfoRegistry.communityInfoFlow.emit(communityState(content = CommunityContent.Loading))

    presenter.test {
      assertThat(awaitItemWithSlot<CommunitySlot>())
        .loadedSlots
        .containsInstance<CommunitySlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_UnavailableContent_StillGeneratesCommunitySlot() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    bookInfoRegistry.communityInfoFlow.emit(communityState(content = CommunityContent.Unavailable))

    presenter.test {
      assertThat(awaitItemWithSlot<CommunitySlot>())
        .loadedSlots
        .containsInstance<CommunitySlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_CommunityInfoWithReviews_GeneratesCommunitySlot() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    bookInfoRegistry.communityInfoFlow.emit(
      communityState(
        content = CommunityContent.Available(
          info = communityInfo,
          reviews = listOf(
            BookReview(author = "reader", rating = 5.0, text = "Loved it", hasSpoilers = false),
          ),
        ),
      ),
    )

    presenter.test {
      assertThat(awaitItemWithSlot<CommunitySlot>())
        .loadedSlots
        .containsInstance<CommunitySlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_NoServableProvider_GeneratesNoSlot() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    bookInfoRegistry.communityInfoFlow.emit(null)

    presenter.test {
      assertThat(awaitLoadedItem())
        .loadedSlots
        .doesNotContainInstance<CommunitySlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_OpenProviderPage_NavigatesToUrl() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    bookInfoRegistry.communityInfoFlow.emit(communityState())

    presenter.test {
      val state = awaitItemWithSlot<CommunitySlot>()
      state.eventSink(
        LibraryItemUiEvent.OpenProviderPage("https://hardcover.app/books/the-way-of-kings"),
      )

      assertThat(navigator.awaitNextScreen())
        .isEqualTo(UrlScreen("https://hardcover.app/books/the-way-of-kings"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_SelectCommunitySource_ResubscribesWithPreferredProvider() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    bookInfoRegistry.communityInfoFlow.emit(communityState())

    presenter.test {
      val state = awaitItemWithSlot<CommunitySlot>()

      state.eventSink(LibraryItemUiEvent.SelectCommunitySource(ProviderId.OpenLibrary))

      awaitItemMatching {
        bookInfoRegistry.communityInfoRequests.lastOrNull()?.second == ProviderId.OpenLibrary
      }
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(bookInfoRegistry.communityInfoRequests.last().second)
      .isEqualTo(ProviderId.OpenLibrary)
  }

  @Test
  fun present_RelinkProvider_NavigatesToConnectedProviders() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    bookInfoRegistry.communityInfoFlow.emit(communityState().copy(needsRelink = true))

    presenter.test {
      val state = awaitItemWithSlot<CommunitySlot>()
      state.eventSink(LibraryItemUiEvent.RelinkProvider)

      assertThat(navigator.awaitNextScreen()).isEqualTo(ConnectedProvidersScreen)
      cancelAndIgnoreRemainingEvents()
    }
  }
}

private suspend fun ReceiveTurbine<LibraryItemUiState>.awaitLoadedItem(): LibraryItemUiState {
  return awaitItemMatching { it.contentState is LoadState.Loaded<*> }
}

private suspend inline fun <reified S : ContentSlot>
  ReceiveTurbine<LibraryItemUiState>.awaitItemWithSlot(): LibraryItemUiState {
  return awaitItemMatching { state ->
    val loaded = state.contentState as? LoadState.Loaded<*> ?: return@awaitItemMatching false
    val content = loaded.data as? ContentUiState ?: return@awaitItemMatching false
    content.slots.any { it is S }
  }
}

private suspend inline fun ReceiveTurbine<LibraryItemUiState>.awaitItemMatching(
  predicate: (LibraryItemUiState) -> Boolean,
): LibraryItemUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}

private val Assert<LibraryItemUiState>.loadedSlots: Assert<List<ContentSlot>>
  get() = prop(LibraryItemUiState::contentState)
    .isInstanceOf<LoadState.Loaded<ContentUiState>>()
    .prop(LoadState.Loaded<ContentUiState>::data)
    .prop(ContentUiState::slots)
