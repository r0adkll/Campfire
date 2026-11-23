// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.bookmarks

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.LibraryItemId
import app.campfire.user.api.BookmarkRepository
import app.campfire.user.test.FakeBookmarkRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class BookmarksBottomSheetTest {

  private val audioPlayerHolder = FakeAudioPlayerHolder()
  private val bookmarkRepository = FakeBookmarkRepository()

  @Test
  fun `new bookmark is offered at the player's position`() = runComposeUiTest {
    audioPlayerHolder.setCurrentPlayer(FakeAudioPlayer().apply { overallTime.value = 90.minutes })
    setSheet()

    onNodeWithText(createLabel(90.minutes)).assertIsDisplayed()
  }

  @Test
  fun `new bookmark is offered at zero without a player`() = runComposeUiTest {
    setSheet()

    onNodeWithText(createLabel(Duration.ZERO)).assertIsDisplayed()
  }

  @Test
  fun `releasing the player lets the sheet go of it`() = runComposeUiTest {
    val player = FakeAudioPlayer().apply { overallTime.value = 90.minutes }
    audioPlayerHolder.setCurrentPlayer(player)
    setSheet()
    onNodeWithText(createLabel(90.minutes)).assertIsDisplayed()

    // What each platform does when playback shuts down
    audioPlayerHolder.release()
    waitForIdle()

    onNodeWithText(createLabel(Duration.ZERO)).assertIsDisplayed()
    assertThat(player.overallTime.subscriptionCount.value).isEqualTo(0)
  }

  private fun createLabel(time: Duration) = "Create new bookmark @ ${time.readoutFormat()}"

  private fun ComposeUiTest.setSheet() {
    val component = object : BookmarksBottomSheetComponent {
      override val audioPlayerHolder: AudioPlayerHolder = this@BookmarksBottomSheetTest.audioPlayerHolder
      override val bookmarkRepository: BookmarkRepository = this@BookmarksBottomSheetTest.bookmarkRepository
    }
    setContent {
      CampfireTheme {
        BookmarksBottomSheet(
          libraryItemId = ITEM_ID,
          onBookmarkClick = {},
          component = component,
        )
      }
    }
  }

  private companion object {
    const val ITEM_ID: LibraryItemId = "item"
  }
}
