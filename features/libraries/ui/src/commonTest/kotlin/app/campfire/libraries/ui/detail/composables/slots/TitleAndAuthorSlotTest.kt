package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.home.ui.libraryItem
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class TitleAndAuthorSlotTest {

  @Test
  fun contentTest() = runComposeUiTest {
    val libraryItem = libraryItem {
      media {
        duration = 5.minutes
        metadata {
          title = "test_title"
          subtitle = "test_subtitle"
          authorName = "test_author_name"
          narratorName = "test_narrator_name"
        }
      }
    }
    val slot = TitleAndAuthorSlot(libraryItem, "")

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("test_title").assertExists()
    onNodeWithText("test_subtitle").assertExists()
    onNodeWithText(5.minutes.readoutFormat()).assertExists()
    onNodeWithText("test_author_name").assertExists()
    onNodeWithText("test_narrator_name").assertExists()
  }
}
