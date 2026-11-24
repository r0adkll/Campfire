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
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRepository
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.test.session
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.home.ui.libraryItem
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.slack.circuit.overlay.OverlayNavigator
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalTestApi::class)
class PlaybackHistoryBottomSheetTest {

  private val repository = FakePlaybackHistoryRepository()
  private val player = FakeAudioPlayer()
  private val audioPlayerHolder = FakeAudioPlayerHolder()
  private val results = mutableListOf<PlaybackHistoryResult>()

  private val play = action(id = 1, type = PlaybackActionType.Play, from = 5.minutes)
  private val finished = action(id = 2, type = PlaybackActionType.Finished, from = 60.minutes)

  @Test
  fun `a finished entry is shown but can't be jumped to`() = runComposeUiTest {
    loadItem()
    setSheet(play, finished)

    onNodeWithText("Finished").assertIsDisplayed()
    onNodeWithText("Finished").assertHasNoClickAction()
    onNodeWithText("Finished").performClick()

    assertThat(results).isEmpty()
  }

  @Test
  fun `other entries jump to their position`() = runComposeUiTest {
    loadItem()
    setSheet(play, finished)

    onNodeWithText("Played").assertHasClickAction()
    onNodeWithText("Played").performClick()

    assertThat(results).containsExactly(PlaybackHistoryResult.Selected(play))
  }

  @Test
  fun `the loaded item's history can be jumped through and cleared`() = runComposeUiTest {
    loadItem()
    setSheet(play)

    onNodeWithText("Tap an event to jump to that position").assertIsDisplayed()
    onNodeWithContentDescription("Clear history").assertIsDisplayed()
  }

  @Test
  fun `an item the player hasn't loaded shows its history only for reading`() = runComposeUiTest {
    // The item's session may still be the active one, after the app was killed
    loadItem(state = AudioPlayer.State.Disabled)
    setSheet(play)

    onNodeWithText("Played").assertIsDisplayed()
    onNodeWithText("Played").assertHasNoClickAction()
    onNodeWithText("Played").performClick()
    onNodeWithText("Tap an event to jump to that position").assertDoesNotExist()
    onNodeWithContentDescription("Clear history").assertDoesNotExist()

    assertThat(results).isEmpty()
  }

  @Test
  fun `the sheet follows the player while it's open`() = runComposeUiTest {
    setSheet(play)
    onNodeWithText("Played").assertHasNoClickAction()

    loadItem()
    waitForIdle()
    onNodeWithText("Played").assertHasClickAction()
    onNodeWithContentDescription("Clear history").assertIsDisplayed()

    player.state.value = AudioPlayer.State.Finished
    waitForIdle()
    onNodeWithText("Played").assertHasNoClickAction()
    onNodeWithContentDescription("Clear history").assertDoesNotExist()
  }

  @Test
  fun `an episode's history is just that episode's, and clearing leaves the others`() = runComposeUiTest {
    val episodePlay = action(id = 3, type = PlaybackActionType.Play, from = 2.minutes, episodeId = EPISODE_ID)
    val otherEpisodePause = action(id = 4, type = PlaybackActionType.Pause, from = 9.minutes, episodeId = "other")
    repository.history[FakePlaybackHistoryRepository.HistoryKey(ITEM_ID, "other")] = listOf(otherEpisodePause)
    loadItem(episodeId = EPISODE_ID)
    setSheet(episodePlay, episodeId = EPISODE_ID)

    onNodeWithText("Played").assertIsDisplayed()
    onNodeWithText("Paused").assertDoesNotExist()

    onNodeWithContentDescription("Clear history").performClick()
    waitForIdle()

    assertThat(runBlocking { repository.get(ITEM_ID, EPISODE_ID) }).isEmpty()
    assertThat(runBlocking { repository.get(ITEM_ID, "other") }).isEqualTo(listOf(otherEpisodePause))
  }

  private fun loadItem(
    state: AudioPlayer.State = AudioPlayer.State.Paused,
    episodeId: PodcastEpisodeId? = null,
  ) {
    player.preparedSession = session(libraryItem = libraryItem(id = ITEM_ID)).copy(episodeId = episodeId)
    player.state.value = state
    audioPlayerHolder.setCurrentPlayer(player)
  }

  private fun ComposeUiTest.setSheet(vararg actions: PlaybackAction, episodeId: PodcastEpisodeId? = null) {
    repository.history[FakePlaybackHistoryRepository.HistoryKey(ITEM_ID, episodeId)] = actions.toList()
    val component = object : PlaybackHistoryBottomSheetComponent {
      override val playbackHistoryRepository: PlaybackHistoryRepository = repository
      override val audioPlayerHolder: AudioPlayerHolder = this@PlaybackHistoryBottomSheetTest.audioPlayerHolder
    }
    val navigator = object : OverlayNavigator<PlaybackHistoryResult> {
      override fun finish(result: PlaybackHistoryResult) {
        results += result
      }
    }
    setContent {
      CampfireTheme {
        PlaybackHistoryBottomSheet(
          input = PlaybackHistoryInput(ITEM_ID, episodeId),
          navigator = navigator,
          component = component,
        )
      }
    }
  }

  private fun action(
    id: Long,
    type: PlaybackActionType,
    from: Duration,
    episodeId: PodcastEpisodeId? = null,
  ) = PlaybackAction(
    id = id,
    libraryItemId = ITEM_ID,
    userId = "user",
    episodeId = episodeId,
    type = type,
    timestamp = LocalDateTime(2026, 1, 1, 12, id.toInt()),
    fromPosition = from,
    toPosition = null,
  )

  private companion object {
    const val ITEM_ID = "item"
    const val EPISODE_ID = "episode"
  }
}
