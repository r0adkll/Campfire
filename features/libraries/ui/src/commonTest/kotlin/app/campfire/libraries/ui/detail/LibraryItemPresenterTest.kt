// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.common.test.assert.containsInstance
import app.campfire.common.test.assert.doesNotContainInstance
import app.campfire.common.test.assert.firstInstanceOf
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.SeriesSequence
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import app.campfire.libraries.ui.detail.composables.slots.ChapterHeaderSlot
import app.campfire.libraries.ui.detail.composables.slots.ChapterSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsSlot
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.campfire.libraries.ui.detail.composables.slots.ExpressiveControlSlot
import app.campfire.libraries.ui.detail.composables.slots.ProgressSlot
import app.campfire.libraries.ui.detail.composables.slots.SeriesSlot
import app.campfire.libraries.ui.detail.composables.slots.SummarySlot
import app.cash.turbine.ReceiveTurbine
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.prop
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class LibraryItemPresenterTest : BaseLibraryItemPresenterTest() {

  @Test
  fun present_Default_LoadingUiState() = runTest {
    presenter.test {
      assertThat(awaitItem()).all {
        prop(LibraryItemUiState::libraryItem).isNull()
        prop(LibraryItemUiState::contentState).isInstanceOf<LoadState.Loading>()
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_EmptyLibraryItem_GeneratesMinimumSlots() = runTest {
    val libraryItem = emptyLibraryItem()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      assertThat(awaitLoadedItem())
        .loadedSlots
        .all {
          doesNotContainInstance<ProgressSlot>()
          doesNotContainInstance<SummarySlot>()
          doesNotContainInstance<SeriesSlot>()
          doesNotContainInstance<ChipsSlot>()
          doesNotContainInstance<ChapterHeaderSlot>()
          doesNotContainInstance<ChapterSlot>()
        }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_MediaProgressState_GeneratesProgressSlot() = runTest {
    val libraryItem = emptyLibraryItem()
    val mediaProgress = mediaProgress()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)
    mediaProgressRepository.progressFlow.value = mediaProgress

    presenter.test {
      assertThat(awaitItemWithSlot<ProgressSlot>())
        .loadedSlots
        .containsInstance<ProgressSlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_Description_GeneratesSummarySlot() = runTest {
    val libraryItem = emptyLibraryItem(description = "Some desc")
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      assertThat(awaitItemWithSlot<SummarySlot>())
        .loadedSlots
        .containsInstance<SummarySlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_Series_GeneratesSeriesSlot() = runTest {
    val libraryItem = libraryItem(
      seriesSequence = SeriesSequence("", "", 0),
    )
    libraryItemRepository.libraryItemFlow.emit(libraryItem)
    val seriesBooks = listOf(libraryItem())
    seriesRepository.seriesLibraryItemsFlow.emit(seriesBooks)

    presenter.test {
      assertThat(awaitItemWithSlot<SeriesSlot>())
        .loadedSlots
        .containsInstance<SeriesSlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_EmptySeries_NoSeriesSlot() = runTest {
    val libraryItem = libraryItem(
      seriesSequence = SeriesSequence("", "", 0),
    )
    libraryItemRepository.libraryItemFlow.emit(libraryItem)
    seriesRepository.seriesLibraryItemsFlow.emit(emptyList())

    presenter.test {
      assertThat(awaitLoadedItem())
        .loadedSlots
        .doesNotContainInstance<SeriesSlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_Genres_GeneratesChipsSlot() = runTest {
    val libraryItem = emptyLibraryItem(
      genres = listOf("genre"),
    )
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      assertThat(awaitItemWithSlot<ChipsSlot>())
        .loadedSlots
        .containsInstance<ChipsSlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_Tags_GeneratesChipsSlot() = runTest {
    val libraryItem = emptyLibraryItem(
      tags = listOf("tag"),
    )
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      assertThat(awaitItemWithSlot<ChipsSlot>())
        .loadedSlots
        .containsInstance<ChipsSlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_Chapters_GeneratesChapterSlot() = runTest {
    val libraryItem = emptyLibraryItem(
      numOfChapters = 20,
    )
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      assertThat(awaitItemWithSlot<ChapterSlot>())
        .loadedSlots
        .all {
          containsInstance<ChapterHeaderSlot>()
          transform { it.filterIsInstance<ChapterSlot>() }
            .hasSize(20)
        }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_showTimeInBook_UpdatesChapterHeaderSlot() = runTest {
    val libraryItem = emptyLibraryItem(numOfChapters = 1)
    settings.showTimeInBook = false
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      assertThat(awaitItemMatching { it.firstChapterHeader()?.showTimeInBook == false })
        .loadedSlots
        .firstInstanceOf<ChapterHeaderSlot>()
        .prop(ChapterHeaderSlot::showTimeInBook)
        .isEqualTo(false)

      settings.showTimeInBook = true

      assertThat(awaitItemMatching { it.firstChapterHeader()?.showTimeInBook == true })
        .loadedSlots
        .firstInstanceOf<ChapterHeaderSlot>()
        .prop(ChapterHeaderSlot::showTimeInBook)
        .isEqualTo(true)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_showConfirmDownloadDialog_UpdatesExpressiveControlSlot() = runTest {
    val libraryItem = emptyLibraryItem()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      assertThat(
        awaitItemMatching {
          it.firstExpressiveControl()?.showConfirmDownloadDialogSetting == false
        },
      )
        .loadedSlots
        .firstInstanceOf<ExpressiveControlSlot>()
        .prop(ExpressiveControlSlot::showConfirmDownloadDialogSetting)
        .isEqualTo(false)

      settings.showConfirmDownload = true

      assertThat(
        awaitItemMatching {
          it.firstExpressiveControl()?.showConfirmDownloadDialogSetting == true
        },
      )
        .loadedSlots
        .firstInstanceOf<ExpressiveControlSlot>()
        .prop(ExpressiveControlSlot::showConfirmDownloadDialogSetting)
        .isEqualTo(true)
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

private fun LibraryItemUiState.firstChapterHeader(): ChapterHeaderSlot? {
  val loaded = contentState as? LoadState.Loaded<*> ?: return null
  val content = loaded.data as? ContentUiState ?: return null
  return content.slots.filterIsInstance<ChapterHeaderSlot>().firstOrNull()
}

private fun LibraryItemUiState.firstExpressiveControl(): ExpressiveControlSlot? {
  val loaded = contentState as? LoadState.Loaded<*> ?: return null
  val content = loaded.data as? ContentUiState ?: return null
  return content.slots.filterIsInstance<ExpressiveControlSlot>().firstOrNull()
}

private val Assert<LibraryItemUiState>.loadedSlots: Assert<List<ContentSlot>>
  get() = prop(LibraryItemUiState::contentState)
    .isInstanceOf<LoadState.Loaded<ContentUiState>>()
    .prop(LoadState.Loaded<ContentUiState>::data)
    .prop(ContentUiState::slots)
