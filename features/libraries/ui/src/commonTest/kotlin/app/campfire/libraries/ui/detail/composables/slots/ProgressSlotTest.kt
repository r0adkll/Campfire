// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.common.test.mediaProgress
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.libraries.ui.detail.TestLibraryItemId
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

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
  }
}
