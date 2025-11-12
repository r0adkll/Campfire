package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.core.model.preview.mediaProgress
import app.campfire.home.ui.libraryItem
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class ControlSlotTest {

  @Test
  fun baseContentTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val slot = ControlSlot(
      libraryItem = libraryItem,
      offlineDownload = null,
      mediaProgress = null,
      showConfirmDownloadDialogSetting = false,
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("button_play")
      .assertExists()
      .assertTextContains("Play")

    onNodeWithTag("button_download").assertExists()
    onNodeWithTag("button_mark_finished").assertExists()
  }

  @Test
  fun hasProgressContentTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val mediaProgress = mediaProgress(
      progress = 0.5f,
      isFinished = false,
    )
    val slot = ControlSlot(
      libraryItem = libraryItem,
      offlineDownload = null,
      mediaProgress = mediaProgress,
      showConfirmDownloadDialogSetting = false,
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("button_play")
      .assertExists()
      .assertTextContains("Continue listening")

    onNodeWithTag("button_discard_progress").assertExists()
    onNodeWithTag("button_mark_finished").assertExists()
  }

  @Test
  fun hasFinishedProgressContentTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val mediaProgress = mediaProgress(
      progress = 1f,
      isFinished = true,
    )
    val slot = ControlSlot(
      libraryItem = libraryItem,
      offlineDownload = null,
      mediaProgress = mediaProgress,
      showConfirmDownloadDialogSetting = false,
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("button_play")
      .assertExists()
      .assertTextContains("Play")

    onNodeWithTag("button_discard_progress").assertDoesNotExist()
    onNodeWithTag("button_mark_finished").assertDoesNotExist()
    onNodeWithTag("button_mark_not_finished").assertExists()
  }
}
