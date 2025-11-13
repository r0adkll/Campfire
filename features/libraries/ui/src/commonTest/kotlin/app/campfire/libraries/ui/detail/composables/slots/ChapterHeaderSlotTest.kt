package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class ChapterHeaderSlotTest {

  @Test
  fun showTimeInBookFalseTest() = runComposeUiTest {
    val slot = ChapterHeaderSlot(showTimeInBook = false)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Chapters").isDisplayed()
    onNode(isToggleable()).assertIsOff()
  }

  @Test
  fun showTimeInBookTrueTest() = runComposeUiTest {
    val slot = ChapterHeaderSlot(showTimeInBook = true)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Chapters").isDisplayed()
    onNode(isToggleable()).assertIsOn()
  }
}
