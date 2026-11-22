// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.history

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRepository
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.PlaybackActionType
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.slack.circuit.overlay.OverlayNavigator
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalTestApi::class)
class PlaybackHistoryBottomSheetTest {

  private val repository = FakePlaybackHistoryRepository()
  private val results = mutableListOf<PlaybackHistoryResult>()

  private val play = action(id = 1, type = PlaybackActionType.Play, from = 5.minutes)
  private val finished = action(id = 2, type = PlaybackActionType.Finished, from = 60.minutes)

  @Test
  fun `a finished entry is shown but can't be jumped to`() = runComposeUiTest {
    setSheet(play, finished)

    onNodeWithText("Finished").assertIsDisplayed()
    onNodeWithText("Finished").assertHasNoClickAction()
    onNodeWithText("Finished").performClick()

    assertThat(results).isEmpty()
  }

  @Test
  fun `other entries jump to their position`() = runComposeUiTest {
    setSheet(play, finished)

    onNodeWithText("Played").assertHasClickAction()
    onNodeWithText("Played").performClick()

    assertThat(results).containsExactly(PlaybackHistoryResult.Selected(play))
  }

  @Test
  fun `the playing item's history can be jumped through and cleared`() = runComposeUiTest {
    setSheet(play)

    onNodeWithText("Tap an event to jump to that position").assertIsDisplayed()
    onNodeWithContentDescription("Clear history").assertIsDisplayed()
  }

  @Test
  fun `history that can't be jumped through is only for reading`() = runComposeUiTest {
    setSheet(play, canJump = false)

    onNodeWithText("Played").assertIsDisplayed()
    onNodeWithText("Played").assertHasNoClickAction()
    onNodeWithText("Played").performClick()
    onNodeWithText("Tap an event to jump to that position").assertDoesNotExist()
    onNodeWithContentDescription("Clear history").assertDoesNotExist()

    assertThat(results).isEmpty()
  }

  private fun ComposeUiTest.setSheet(vararg actions: PlaybackAction, canJump: Boolean = true) {
    repository.history[FakePlaybackHistoryRepository.HistoryKey(ITEM_ID)] = actions.toList()
    val component = object : PlaybackHistoryBottomSheetComponent {
      override val playbackHistoryRepository: PlaybackHistoryRepository = repository
    }
    val navigator = object : OverlayNavigator<PlaybackHistoryResult> {
      override fun finish(result: PlaybackHistoryResult) {
        results += result
      }
    }
    setContent {
      CampfireTheme {
        PlaybackHistoryBottomSheet(
          libraryItemId = ITEM_ID,
          canJump = canJump,
          navigator = navigator,
          component = component,
        )
      }
    }
  }

  private fun action(id: Long, type: PlaybackActionType, from: Duration) = PlaybackAction(
    id = id,
    libraryItemId = ITEM_ID,
    userId = "user",
    type = type,
    timestamp = LocalDateTime(2026, 1, 1, 12, id.toInt()),
    fromPosition = from,
    toPosition = null,
  )

  private companion object {
    const val ITEM_ID = "item"
  }
}
