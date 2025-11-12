package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.common.test.mediaProgress
import app.campfire.libraries.ui.detail.TestLibraryItemId
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class ProgressSlotTest {

  @Test
  fun inProgressTest() = runComposeUiTest {
    val mediaProgress = mediaProgress(
      libraryItemId = TestLibraryItemId,
      currentTime = 20f,
      duration = 100f,
    )
    val slot = ProgressSlot(mediaProgress)

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
  fun isFinishedTest() = runComposeUiTest {
    val mediaProgress = mediaProgress(
      libraryItemId = TestLibraryItemId,
      currentTime = 100f,
      duration = 100f,
      isFinished = true,
      finishedAt = 0L,
    )
    val slot = ProgressSlot(mediaProgress)

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
