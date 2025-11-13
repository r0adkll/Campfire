package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class SummarySlotTest {

  @Test
  fun contentTest() = runComposeUiTest {
    val description = "test_content_desc"
    val slot = SummarySlot(description)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Summary").isDisplayed()
    onNodeWithText(description).isDisplayed()
    onNodeWithTag("button_show_more_less").assertDoesNotExist()
  }

  @Test
  fun longContentTest() = runComposeUiTest {
    val description = "description Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor" +
      " incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco" +
      " laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit" +
      " esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa" +
      " qui officia deserunt mollit anim id est laborum."
    val slot = SummarySlot(description.repeat(6))

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    awaitIdle()

    onNodeWithText("Summary").isDisplayed()
    onNodeWithText("description", substring = true).isDisplayed()
    val moreLessButton = onNodeWithTag("button_show_more_less")
      .assertTextEquals("Show more")
      .performClick()

    moreLessButton.assertTextEquals("Show less")
  }
}
