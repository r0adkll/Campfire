package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class PublishedSlotTest {

  @Test
  fun contentTest() = runComposeUiTest {
    val slot = PublishedSlot("publisher", null)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Published by").isDisplayed()
    onNodeWithText("publisher").isDisplayed()
  }

  @Test
  fun contentWithYearTest() = runComposeUiTest {
    val slot = PublishedSlot("publisher", "2025")

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Published by").isDisplayed()
    onNodeWithText("publisher in 2025").isDisplayed()
  }
}
