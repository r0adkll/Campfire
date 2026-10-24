// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.test.mediaProgress
import app.campfire.core.extensions.asDate
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.TestLibraryItemId
import app.campfire.libraries.ui.detail.dates.ListeningDate
import assertk.assertThat
import assertk.assertions.containsExactly
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test
import kotlin.time.Clock
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class ProgressSlotTest {

  @Test
  fun inProgressTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val mediaProgress = mediaProgress(
      libraryItemId = TestLibraryItemId,
      currentTime = 20f,
      duration = 100f,
    )
    val slot = ProgressSlot(false, 1f, mediaProgress, libraryItem)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("progress_indicator").assertExists()
    onNodeWithTag("icon_finished_check").assertDoesNotExist()
    onNodeWithText("1m 20s left").assertExists()
    onNodeWithText("20%").assertExists()
  }

  @Test
  fun ebookReadsToTheTenthTest() = runComposeUiTest {
    val libraryItem = libraryItem(
      media = media(numTracks = 0, numAudioFiles = 0, durationInMillis = 0L, ebookFormat = "epub"),
    )
    val mediaProgress = mediaProgress(
      libraryItemId = TestLibraryItemId,
      currentTime = 0f,
      duration = 0f,
      ebookProgress = 0.225f,
    )
    val slot = ProgressSlot(false, 1f, mediaProgress, libraryItem)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("22.5%").assertExists()
    onNodeWithText("left", substring = true).assertDoesNotExist()
  }

  @Test
  fun readingBarOfAnAudiobookShowsOnlyItsReadingPlace() = runComposeUiTest {
    val mediaProgress = mediaProgress(
      libraryItemId = TestLibraryItemId,
      currentTime = 20f,
      duration = 100f,
      ebookProgress = 0.225f,
    )
    val slot =
      ProgressSlot(false, 1f, mediaProgress, libraryItem(), chapter = "Chapter 7", kind = ProgressSlot.Kind.Reading)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Chapter 7").assertExists()
    onNodeWithText("22.5%").assertExists()
    onNodeWithText("left", substring = true).assertDoesNotExist()
  }

  @Test
  fun listeningBarOfAnAudiobookIgnoresItsReadingPlace() = runComposeUiTest {
    val mediaProgress = mediaProgress(
      libraryItemId = TestLibraryItemId,
      currentTime = 20f,
      duration = 100f,
      ebookProgress = 0.9f,
    )
    val slot = ProgressSlot(false, 1f, mediaProgress, libraryItem(), kind = ProgressSlot.Kind.Listening)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("1m 20s left").assertExists()
    onNodeWithText("20%").assertExists()
  }

  @Test
  fun isAcceleratedTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val mediaProgress = mediaProgress(
      libraryItemId = TestLibraryItemId,
      currentTime = 20f,
      duration = 100f,
    )
    val slot = ProgressSlot(false, 2f, mediaProgress, libraryItem)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("progress_indicator").assertExists()
    onNodeWithTag("icon_finished_check").assertDoesNotExist()
    onNodeWithTag("accelerated_icon").assertExists()
    onNodeWithText("40s left").assertExists()
    onNodeWithText("20%").assertExists()
  }

  @Test
  fun isFinishedTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val mediaProgress = mediaProgress(
      libraryItemId = TestLibraryItemId,
      currentTime = 100f,
      duration = 100f,
      isFinished = true,
      finishedAt = 0L,
    )
    val slot = ProgressSlot(false, 1f, mediaProgress, libraryItem)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("progress_indicator").assertExists()
    onNodeWithTag("icon_finished_check").assertExists()
    onNodeWithText("Finished on", substring = true).assertExists()
    onNodeWithText("100%").assertExists()
    // Without the dates to change, the bar is only a bar
    onNodeWithTag("progress_edit_dates").assertDoesNotExist()
  }

  @Test
  fun finishedWithoutAFinishDateTest() = runComposeUiTest {
    val mediaProgress = mediaProgress(
      libraryItemId = TestLibraryItemId,
      currentTime = 100f,
      duration = 100f,
      isFinished = true,
      finishedAt = null,
    )
    val slot = ProgressSlot(false, 1f, mediaProgress, libraryItem())

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Finished").assertExists()
  }

  @Test
  fun editableBarOpensTheListeningDatesTest() = runComposeUiTest {
    val events = mutableListOf<LibraryItemUiEvent>()
    val slot = ProgressSlot(false, 1f, finishedProgress(), libraryItem(), canEditDates = true)

    setContent {
      PreviewSharedElementTransitionLayout {
        CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800)) {
          ContentWithOverlays {
            slot.Content(Modifier) { events += it }
          }
        }
      }
    }

    // The bar and the line under it are one button
    onNodeWithTag("progress_edit_dates")
      .assertHasClickAction()
      .assert(hasText("Finished on", substring = true))
      .performClick()

    waitUntil { onAllNodesWithText("Listening dates").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("listening_date_started").assertHasNoClickAction()
    onNodeWithTag("listening_date_finished").performClick()

    waitUntil { events.isNotEmpty() }
    assertThat(events).containsExactly(LibraryItemUiEvent.EditListeningDate(ListeningDate.Finished))
  }

  @Test
  fun pickingTheFinishedDateTest() = runComposeUiTest {
    val events = mutableListOf<LibraryItemUiEvent>()
    val progress = finishedProgress()
    val slot = ProgressSlot(
      isPlaying = false,
      playbackSpeed = 1f,
      mediaProgress = progress,
      libraryItem = libraryItem(),
      canEditDates = true,
      editingDate = ListeningDate.Finished,
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800)) {
          ContentWithOverlays {
            slot.Content(Modifier) { events += it }
          }
        }
      }
    }

    onNodeWithText("Finished on").assertExists()
    onNodeWithText("earlier days can't be picked", substring = true).assertExists()
    onNodeWithTag("date_picker_confirm").performClick()

    assertThat(events).containsExactly(
      LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Finished, progress.finishedAt!!.asDate()),
    )
  }

  @Test
  fun theStartCanBeChangedFromTheListeningDatesTest() = runComposeUiTest {
    val events = mutableListOf<LibraryItemUiEvent>()
    val slot = ProgressSlot(
      isPlaying = false,
      playbackSpeed = 1f,
      mediaProgress = finishedProgress(),
      libraryItem = libraryItem(),
      canEditDates = true,
      canChangeStarted = true,
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800)) {
          ContentWithOverlays {
            slot.Content(Modifier) { events += it }
          }
        }
      }
    }

    onNodeWithTag("progress_edit_dates").performClick()
    waitUntil { onAllNodesWithText("Changing it recreates your progress").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("listening_date_started").performClick()

    waitUntil { events.isNotEmpty() }
    assertThat(events).containsExactly(LibraryItemUiEvent.EditListeningDate(ListeningDate.Started))
  }

  @Test
  fun whilePlayingTheStartSaysToPauseTest() = runComposeUiTest {
    val slot = ProgressSlot(false, 1f, finishedProgress(), libraryItem(), canEditDates = true, canChangeStarted = false)

    setContent {
      PreviewSharedElementTransitionLayout {
        CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800)) {
          ContentWithOverlays {
            slot.Content(Modifier) {}
          }
        }
      }
    }

    onNodeWithTag("progress_edit_dates").performClick()
    waitUntil { onAllNodesWithText("Pause this book to change it").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("listening_date_started").assertHasNoClickAction()
  }

  @Test
  fun aNewStartAsksFirstTest() = runComposeUiTest {
    val events = mutableListOf<LibraryItemUiEvent>()
    val slot = ProgressSlot(
      isPlaying = false,
      playbackSpeed = 1f,
      mediaProgress = finishedProgress(),
      libraryItem = libraryItem(),
      canEditDates = true,
      canChangeStarted = true,
      confirmingStartedOn = LocalDate(2026, 1, 12),
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        ContentWithOverlays {
          slot.Content(Modifier) { events += it }
        }
      }
    }

    onNodeWithText("Change the start date?").assertExists()
    onNodeWithTag("recreate_progress_new_start").assertTextEquals("January 12, 2026")
    onNodeWithTag("recreate_progress_confirm").performClick()

    assertThat(events).containsExactly(LibraryItemUiEvent.ConfirmStartedDate)
  }

  @Test
  fun aProgressLeftOffTheServerOffersToPutItBackTest() = runComposeUiTest {
    val events = mutableListOf<LibraryItemUiEvent>()
    val slot = ProgressSlot(
      isPlaying = false,
      playbackSpeed = 1f,
      mediaProgress = finishedProgress(),
      libraryItem = libraryItem(),
      canEditDates = true,
      progressNotRestored = true,
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        ContentWithOverlays {
          slot.Content(Modifier) { events += it }
        }
      }
    }

    onNodeWithTag("progress_not_restored").assertExists()
    onNodeWithText("Try again").performClick()

    assertThat(events).containsExactly(LibraryItemUiEvent.RestoreProgress)
  }

  private fun finishedProgress() = mediaProgress(
    libraryItemId = TestLibraryItemId,
    currentTime = 100f,
    duration = 100f,
    isFinished = true,
    startedAt = Clock.System.now().toEpochMilliseconds() - 20L * DAY,
    finishedAt = Clock.System.now().toEpochMilliseconds() - 2L * DAY,
  )

  private companion object {
    const val DAY = 86_400_000L
  }
}
