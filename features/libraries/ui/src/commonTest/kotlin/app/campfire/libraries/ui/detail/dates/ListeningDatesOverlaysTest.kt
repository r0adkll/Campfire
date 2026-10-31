// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.test.mediaProgress
import app.campfire.core.extensions.asDate
import app.campfire.libraries.ui.detail.TestLibraryItemId
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test
import kotlin.time.Clock
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class ListeningDatesOverlaysTest {

  @Test
  fun theSheetHandsBackTheDatePickedToChange() = runComposeUiTest {
    var picked: Result<ListeningDate?>? = null

    setContent {
      WithOverlays {
        picked = Result.success(showListeningDatesSheet("The Martian", finishedProgress(), canChangeStarted = true))
      }
    }

    waitUntil { onAllNodesWithText("Listening dates").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithText("The Martian").assertExists()
    onNodeWithText("Changing it recreates your progress").assertExists()
    onNodeWithTag("listening_date_started").performClick()

    waitUntil { picked != null }
    assertThat(picked).isEqualTo(Result.success(ListeningDate.Started))
  }

  @Test
  fun whilePlayingTheSheetSaysToPauseToChangeTheStart() = runComposeUiTest {
    var picked: Result<ListeningDate?>? = null

    setContent {
      WithOverlays {
        picked = Result.success(showListeningDatesSheet("The Martian", finishedProgress(), canChangeStarted = false))
      }
    }

    waitUntil { onAllNodesWithText("Pause this book to change it").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("listening_date_started").assertHasNoClickAction()
    onNodeWithTag("listening_date_finished").performClick()

    waitUntil { picked != null }
    assertThat(picked).isEqualTo(Result.success(ListeningDate.Finished))
  }

  @Test
  fun theFinishedPickerHandsBackTheDayPicked() = runComposeUiTest {
    val progress = finishedProgress()
    var picked: Result<LocalDate?>? = null

    setContent {
      WithOverlays { picked = Result.success(showFinishedDatePicker(progress, today = today())) }
    }

    onNodeWithText("Finished on").assertExists()
    onNodeWithText("earlier days can't be picked", substring = true).assertExists()
    onNodeWithTag("date_picker_confirm").performClick()

    waitUntil { picked != null }
    assertThat(picked).isEqualTo(Result.success(progress.finishedAt!!.asDate()))
  }

  @Test
  fun puttingThePickerAwayHandsBackNothing() = runComposeUiTest {
    var picked: Result<LocalDate?>? = null

    setContent {
      WithOverlays { picked = Result.success(showFinishedDatePicker(finishedProgress(), today = today())) }
    }

    onNodeWithText("Cancel").performClick()

    waitUntil { picked != null }
    assertThat(picked).isEqualTo(Result.success(null))
  }

  @Test
  fun theStartedPickerHandsBackTheDayPicked() = runComposeUiTest {
    val progress = finishedProgress()
    var picked: Result<LocalDate?>? = null

    setContent {
      WithOverlays { picked = Result.success(showStartedDatePicker(progress, today = today())) }
    }

    onNodeWithText("Started on").assertExists()
    onNodeWithText("later days can't be picked", substring = true).assertExists()
    onNodeWithTag("date_picker_confirm").performClick()

    waitUntil { picked != null }
    assertThat(picked).isEqualTo(Result.success(progress.startedAt.asDate()))
  }

  @Test
  fun recreatingTheProgressGoesAheadOnlyWhenAgreed() = runComposeUiTest {
    var agreed: Boolean? = null

    setContent {
      WithOverlays { agreed = showRecreateProgressDialog(from = LocalDate(2026, 1, 2), to = LocalDate(2026, 1, 12)) }
    }

    onNodeWithText("Change the start date?").assertExists()
    onNodeWithTag("recreate_progress_new_start").assertTextEquals("January 12, 2026")
    onNodeWithTag("recreate_progress_confirm").performClick()

    waitUntil { agreed != null }
    assertThat(agreed).isEqualTo(true)
  }

  @Test
  fun backingOutOfRecreatingTheProgressSaysNo() = runComposeUiTest {
    var agreed: Boolean? = null

    setContent {
      WithOverlays { agreed = showRecreateProgressDialog(from = null, to = LocalDate(2026, 1, 12)) }
    }

    onNodeWithText("Cancel").performClick()

    waitUntil { agreed != null }
    assertThat(agreed).isEqualTo(false)
  }

  /** Shows overlays on a phone-sized window, running [show] once they can be. */
  @Composable
  private fun WithOverlays(show: suspend OverlayHost.() -> Unit) {
    PreviewSharedElementTransitionLayout {
      CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800)) {
        ContentWithOverlays {
          val overlayHost = LocalOverlayHost.current
          LaunchedEffect(overlayHost) { overlayHost.show() }
        }
      }
    }
  }

  private fun today(): LocalDate = Clock.System.now().toEpochMilliseconds().asDate()

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
