// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.common.test.assert.containsInstance
import app.campfire.common.test.assert.doesNotContainInstance
import app.campfire.common.test.assert.firstInstanceOf
import app.campfire.common.test.mediaProgress as testMediaProgress
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Media
import app.campfire.core.model.SeriesSequence
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import app.campfire.libraries.ui.detail.composables.slots.ChapterHeaderSlot
import app.campfire.libraries.ui.detail.composables.slots.ChapterSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsSlot
import app.campfire.libraries.ui.detail.composables.slots.CollapsedChapterSlot
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.campfire.libraries.ui.detail.composables.slots.EbookContentsEntrySlot
import app.campfire.libraries.ui.detail.composables.slots.EbookContentsHeaderSlot
import app.campfire.libraries.ui.detail.composables.slots.ExpressiveControlSlot
import app.campfire.libraries.ui.detail.composables.slots.ProgressSlot
import app.campfire.libraries.ui.detail.composables.slots.SeriesSlot
import app.campfire.libraries.ui.detail.composables.slots.SplitAttributionSlot
import app.campfire.libraries.ui.detail.composables.slots.SummarySlot
import app.campfire.libraries.ui.detail.composables.slots.TitleSlot
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.reader.api.EbookContentsEntry
import app.campfire.reader.api.screen.ReaderScreen
import app.cash.turbine.ReceiveTurbine
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
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
      seriesSequence = SeriesSequence("", "", 0.0),
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
      seriesSequence = SeriesSequence("", "", 0.0),
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
    libraryViewSettings.showTimeInBook = false
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      assertThat(awaitItemMatching { it.firstChapterHeader()?.showTimeInBook == false })
        .loadedSlots
        .firstInstanceOf<ChapterHeaderSlot>()
        .prop(ChapterHeaderSlot::showTimeInBook)
        .isEqualTo(false)

      libraryViewSettings.showTimeInBook = true

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

      libraryViewSettings.showConfirmDownload = true

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

  @Test
  fun present_downloadPermission_UpdatesExpressiveControlSlot() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())

    presenter.test {
      awaitItemMatching { it.firstExpressiveControl()?.canDownload == true }

      val user = userRepository.currentStatefulUserFlow.value
      userRepository.currentStatefulUserFlow.value = user.copy(
        permissions = user.permissions.copy(download = false),
      )

      assertThat(awaitItemMatching { it.firstExpressiveControl()?.canDownload == false })
        .loadedSlots
        .firstInstanceOf<ExpressiveControlSlot>()
        .prop(ExpressiveControlSlot::canDownload)
        .isEqualTo(false)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_willStreamHls_UpdatesTitleAndControlSlots() = runTest {
    val libraryItem = emptyLibraryItem()
    streamingRoutePredictor.canStreamHls = true
    streamingRoutePredictor.wouldStreamHlsFlow.value = true
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      assertThat(awaitItemMatching { it.firstExpressiveControl()?.willStreamHls == true })
        .loadedSlots
        .all {
          firstInstanceOf<TitleSlot>()
            .prop(TitleSlot::showHlsBadge)
            .isEqualTo(true)
          firstInstanceOf<ExpressiveControlSlot>().all {
            prop(ExpressiveControlSlot::willStreamHls).isEqualTo(true)
            prop(ExpressiveControlSlot::canStreamHls).isEqualTo(true)
          }
        }

      // The decision re-evaluates live as the underlying settings change
      streamingRoutePredictor.wouldStreamHlsFlow.value = false

      assertThat(awaitItemMatching { it.firstExpressiveControl()?.willStreamHls == false })
        .loadedSlots
        .all {
          firstInstanceOf<TitleSlot>()
            .prop(TitleSlot::showHlsBadge)
            .isEqualTo(false)
          firstInstanceOf<ExpressiveControlSlot>()
            .prop(ExpressiveControlSlot::willStreamHls)
            .isEqualTo(false)
        }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_willStreamHls_SuppressedForDownloadedItems() = runTest {
    val libraryItem = emptyLibraryItem()
    streamingRoutePredictor.canStreamHls = true
    streamingRoutePredictor.wouldStreamHlsFlow.value = true
    libraryItemRepository.libraryItemFlow.emit(libraryItem)
    downloads.downloads.value = listOf(
      Download(
        content = DownloadContent.Audiobook(libraryItem.id),
        state = Download.State.Downloaded,
        bytes = 1L,
        totalBytes = 1L,
      ),
    )

    presenter.test {
      // Downloads play locally, so a completed download suppresses the stream indication
      assertThat(
        awaitItemMatching {
          val control = it.firstExpressiveControl()
          control != null && !control.willStreamHls
        },
      )
        .loadedSlots
        .all {
          firstInstanceOf<TitleSlot>()
            .prop(TitleSlot::showHlsBadge)
            .isEqualTo(false)
          firstInstanceOf<ExpressiveControlSlot>()
            .prop(ExpressiveControlSlot::willStreamHls)
            .isEqualTo(false)
        }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_willStreamHls_NotSuppressedForFailedDownloads() = runTest {
    val libraryItem = emptyLibraryItem()
    streamingRoutePredictor.canStreamHls = true
    streamingRoutePredictor.wouldStreamHlsFlow.value = true
    libraryItemRepository.libraryItemFlow.emit(libraryItem)
    downloads.downloads.value = listOf(
      Download(
        content = DownloadContent.Audiobook(libraryItem.id),
        state = Download.State.Failed,
        bytes = 1L,
        totalBytes = 1L,
      ),
    )

    presenter.test {
      // Only a finished download plays locally; a failed one still streams
      assertThat(
        awaitItemMatching {
          val control = it.firstExpressiveControl()
          control != null && control.willStreamHls
        },
      )
        .loadedSlots
        .all {
          firstInstanceOf<TitleSlot>()
            .prop(TitleSlot::showHlsBadge)
            .isEqualTo(true)
          firstInstanceOf<ExpressiveControlSlot>()
            .prop(ExpressiveControlSlot::willStreamHls)
            .isEqualTo(true)
        }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_canStreamHls_IndependentOfRouteDecision() = runTest {
    val libraryItem = emptyLibraryItem()
    streamingRoutePredictor.canStreamHls = true
    streamingRoutePredictor.wouldStreamHlsFlow.value = false
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    presenter.test {
      // The play-options menu shows whenever HLS is possible, even when the current
      // method wouldn't route to it
      assertThat(awaitItemMatching { it.firstExpressiveControl()?.canStreamHls == true })
        .loadedSlots
        .firstInstanceOf<ExpressiveControlSlot>()
        .all {
          prop(ExpressiveControlSlot::canStreamHls).isEqualTo(true)
          prop(ExpressiveControlSlot::willStreamHls).isEqualTo(false)
        }
      cancelAndIgnoreRemainingEvents()
    }
  }
}

class EbookContentsPresenterTest : BaseLibraryItemPresenterTest() {

  private val prologue = EbookContentsEntry("Prologue", "prologue.xhtml", depth = 0, fraction = 0f)
  private val one = EbookContentsEntry("Chapter 1", "ch1.xhtml", depth = 0, fraction = 0.3f)
  private val two = EbookContentsEntry("Chapter 2", "ch2.xhtml", depth = 0, fraction = 0.6f)

  @Test
  fun ebook_showsItsContents_andTheChapterBeingRead() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem(numTracks = 0, ebookFormat = "epub"))
    ebookContentsRepository.contents.value = listOf(prologue, one, two)
    mediaProgressRepository.progressFlow.value = testMediaProgress(
      libraryItemId = TestLibraryItemId,
      duration = 0f,
      ebookProgress = 0.4f,
    )

    presenter.test {
      val slots = awaitItemWithSlot<EbookContentsEntrySlot>()
      assertThat(slots).loadedSlots.all {
        containsInstance<EbookContentsHeaderSlot>()
        transform { it.filterIsInstance<EbookContentsEntrySlot>().map(EbookContentsEntrySlot::state) }
          .containsExactly(
            EbookContentsEntrySlot.State.Read,
            EbookContentsEntrySlot.State.Current,
            EbookContentsEntrySlot.State.Unread,
          )
        firstInstanceOf<ProgressSlot>().prop(ProgressSlot::chapter).isEqualTo("Chapter 1")
        // A third of the way from Chapter 1 (30%) to Chapter 2 (60%)
        transform { it.filterIsInstance<EbookContentsEntrySlot>()[1].progressInEntry }
          .isCloseTo(1f / 3f, 0.001f)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun ebook_collapsesTheEntriesReadPast_untilExpanded() = runTest {
    val three = EbookContentsEntry("Chapter 3", "ch3.xhtml", depth = 0, fraction = 0.8f)
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem(numTracks = 0, ebookFormat = "epub"))
    ebookContentsRepository.contents.value = listOf(prologue, one, two, three)
    mediaProgressRepository.progressFlow.value = testMediaProgress(
      libraryItemId = TestLibraryItemId,
      duration = 0f,
      ebookProgress = 0.7f,
    )

    presenter.test {
      val collapsed = awaitItemWithSlot<EbookContentsEntrySlot>()
      assertThat(collapsed).loadedSlots.all {
        transform { it.filterIsInstance<EbookContentsEntrySlot>().map(EbookContentsEntrySlot::state) }
          .containsExactly(
            EbookContentsEntrySlot.State.Read,
            EbookContentsEntrySlot.State.Current,
            EbookContentsEntrySlot.State.Unread,
          )
        // The folded row sits between the first entry and the one being read
        transform { slots -> slots[slots.indexOfFirst { it is EbookContentsEntrySlot } + 1] }
          .isInstanceOf<CollapsedChapterSlot>()
      }

      collapsed.eventSink(LibraryItemUiEvent.ExpandChaptersClick)

      val expanded = awaitItemMatching { state ->
        val content = (state.contentState as? LoadState.Loaded<*>)?.data as? ContentUiState
        content?.slots?.count { it is EbookContentsEntrySlot } == 4
      }
      assertThat(expanded).loadedSlots.doesNotContainInstance<CollapsedChapterSlot>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun readAt_opensTheReaderAtTheEntry() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem(numTracks = 0, ebookFormat = "epub"))

    presenter.test {
      awaitLoadedItem().eventSink(LibraryItemUiEvent.ReadAt("ch2.xhtml"))

      assertThat(navigator.awaitNextScreen()).isEqualTo(ReaderScreen(TestLibraryItemId, href = "ch2.xhtml"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun audiobookWithAnEbook_showsListeningAndReadingApart() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem(ebookFormat = "epub"))
    ebookContentsRepository.contents.value = listOf(prologue, one, two)
    mediaProgressRepository.progressFlow.value = testMediaProgress(
      libraryItemId = TestLibraryItemId,
      duration = 100f,
      currentTime = 34f,
      progress = 0.34f,
      ebookProgress = 0.65f,
    )

    presenter.test {
      val slots = awaitItemWithSlot<ProgressSlot>()
      assertThat(slots).loadedSlots
        .transform { it.filterIsInstance<ProgressSlot>().map { slot -> slot.kind to slot.chapter } }
        .containsExactly(
          ProgressSlot.Kind.Listening to null,
          ProgressSlot.Kind.Reading to "Chapter 2",
        )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun audiobookWithAnEbook_onlyRead_showsOnlyReading() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem(ebookFormat = "epub"))
    mediaProgressRepository.progressFlow.value = testMediaProgress(
      libraryItemId = TestLibraryItemId,
      duration = 100f,
      ebookProgress = 0.2f,
    )

    presenter.test {
      val slots = awaitItemWithSlot<ProgressSlot>()
      assertThat(slots).loadedSlots
        .transform { it.filterIsInstance<ProgressSlot>().map(ProgressSlot::kind) }
        .containsExactly(ProgressSlot.Kind.Reading)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun ebookWithAnEmptyNarratorName_showsOnlyTheAuthorByline() = runTest {
    val ebook = emptyLibraryItem(numTracks = 0, ebookFormat = "epub")
    val media = ebook.media as Media.Book
    libraryItemRepository.libraryItemFlow.emit(
      ebook.copy(media = media.copy(metadata = media.metadata.copy(narratorName = "", narrators = emptyList()))),
    )

    presenter.test {
      assertThat(awaitLoadedItem()).loadedSlots
        .firstInstanceOf<SplitAttributionSlot>()
        .prop(SplitAttributionSlot::showsRightByline)
        .isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun audiobook_hasNoEbookContents() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem(ebookFormat = "epub"))
    ebookContentsRepository.contents.value = listOf(prologue, one)

    presenter.test {
      assertThat(awaitLoadedItem()).loadedSlots.all {
        doesNotContainInstance<EbookContentsHeaderSlot>()
        doesNotContainInstance<EbookContentsEntrySlot>()
      }
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
