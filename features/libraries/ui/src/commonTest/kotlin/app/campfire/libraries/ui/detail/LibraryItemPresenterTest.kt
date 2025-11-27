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
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
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
        prop(LibraryItemUiState::showConfirmDownloadDialog).isTrue()
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_EmptyLibraryItem_GeneratesMinimumSlots() = runTest {
    val libraryItem = emptyLibraryItem()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      skipItems(1)
      assertThat(awaitItem())
        .loadedSlots
        .all {
          doesNotContainInstance<ProgressSlot>()
          doesNotContainInstance<SummarySlot>()
          doesNotContainInstance<SeriesSlot>()
          doesNotContainInstance<ChipsSlot>()
          doesNotContainInstance<ChapterHeaderSlot>()
          doesNotContainInstance<ChapterSlot>()
        }
    }
  }

  @Test
  fun present_MediaProgressState_GeneratesProgressSlot() = runTest {
    val libraryItem = emptyLibraryItem()
    val mediaProgress = mediaProgress()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)
    mediaProgressRepository.progressFlow.value = mediaProgress

    presenter.test {
      skipItems(1)
      assertThat(awaitItem())
        .loadedSlots
        .containsInstance<ProgressSlot>()
    }
  }

  @Test
  fun present_Description_GeneratesSummarySlot() = runTest {
    val libraryItem = emptyLibraryItem(description = "Some desc")
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      skipItems(1)
      assertThat(awaitItem())
        .loadedSlots
        .containsInstance<SummarySlot>()
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
      skipItems(1)
      assertThat(awaitItem())
        .loadedSlots
        .containsInstance<SeriesSlot>()
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
      skipItems(1)
      assertThat(awaitItem())
        .loadedSlots
        .doesNotContainInstance<SeriesSlot>()
    }
  }

  @Test
  fun present_Genres_GeneratesChipsSlot() = runTest {
    val libraryItem = emptyLibraryItem(
      genres = listOf("genre"),
    )
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      skipItems(1)
      assertThat(awaitItem())
        .loadedSlots
        .containsInstance<ChipsSlot>()
    }
  }

  @Test
  fun present_Tags_GeneratesChipsSlot() = runTest {
    val libraryItem = emptyLibraryItem(
      tags = listOf("tag"),
    )
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      skipItems(1)
      assertThat(awaitItem())
        .loadedSlots
        .containsInstance<ChipsSlot>()
    }
  }

  @Test
  fun present_Chapters_GeneratesChapterSlot() = runTest {
    val libraryItem = emptyLibraryItem(
      numOfChapters = 20,
    )
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      skipItems(1)
      assertThat(awaitItem())
        .loadedSlots
        .all {
          containsInstance<ChapterHeaderSlot>()
          transform { it.filterIsInstance<ChapterSlot>() }
            .hasSize(20)
        }
    }
  }

  @Test
  fun present_showTimeInBook_UpdatesChapterHeaderSlot() = runTest {
    val libraryItem = emptyLibraryItem(numOfChapters = 1)
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      skipItems(1)

      assertThat(awaitItem())
        .loadedSlots
        .firstInstanceOf<ChapterHeaderSlot>()
        .prop(ChapterHeaderSlot::showTimeInBook)
        .isEqualTo(false)

      settings.showTimeInBook = true

      assertThat(awaitItem())
        .loadedSlots
        .firstInstanceOf<ChapterHeaderSlot>()
        .prop(ChapterHeaderSlot::showTimeInBook)
        .isEqualTo(true)
    }
  }

  @Test
  fun present_showConfirmDownloadDialog_UpdatesState() = runTest {
    val libraryItem = emptyLibraryItem()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      skipItems(1)

      assertThat(awaitItem())
        .prop(LibraryItemUiState::showConfirmDownloadDialog)
        .isEqualTo(false)

      settings.showConfirmDownload = true

      assertThat(awaitItem())
        .prop(LibraryItemUiState::showConfirmDownloadDialog)
        .isEqualTo(true)
    }
  }

  @Test
  fun present_showConfirmDownloadDialog_UpdatesExpressiveControlSlot() = runTest {
    val libraryItem = emptyLibraryItem()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      skipItems(1)

      assertThat(awaitItem())
        .loadedSlots
        .firstInstanceOf<ExpressiveControlSlot>()
        .prop(ExpressiveControlSlot::showConfirmDownloadDialogSetting)
        .isEqualTo(false)

      settings.showConfirmDownload = true

      assertThat(awaitItem())
        .loadedSlots
        .firstInstanceOf<ExpressiveControlSlot>()
        .prop(ExpressiveControlSlot::showConfirmDownloadDialogSetting)
        .isEqualTo(true)
    }
  }
}

private val Assert<LibraryItemUiState>.loadedSlots: Assert<List<ContentSlot>>
  get() = prop(LibraryItemUiState::contentState)
    .isInstanceOf<LoadState.Loaded<List<ContentSlot>>>()
    .prop(LoadState.Loaded<List<ContentSlot>>::data)
