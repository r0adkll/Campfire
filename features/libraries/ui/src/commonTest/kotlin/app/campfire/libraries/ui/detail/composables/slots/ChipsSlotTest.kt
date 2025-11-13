package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.genres_title
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class ChipsSlotTest {

  @Test
  fun contentTest() = runComposeUiTest {
    val slot = ChipsSlot(
      title = ChipsTitle(Res.plurals.genres_title, 5),
      chips = List(5) { "chip: $it" },
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Genres").isDisplayed()
    onAllNodesWithText("chip:", substring = true)
      .assertCountEquals(5)
  }
}
