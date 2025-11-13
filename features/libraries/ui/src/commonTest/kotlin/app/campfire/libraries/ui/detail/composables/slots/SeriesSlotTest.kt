package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.libraryItem
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class SeriesSlotTest {

  @Test
  fun seriesNameContentTest() = runComposeUiTest {
    val libraryItem = libraryItem {
      media {
        metadata {
          seriesSequence = null
          seriesName = "test_series_name"
        }
      }
    }
    val seriesBooks = listOf(libraryItem())
    val slot = SeriesSlot(libraryItem, seriesBooks)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Series").isDisplayed()
    onNodeWithText("test_series_name").isDisplayed()
  }

  @Test
  fun seriesSequenceContentTest() = runComposeUiTest {
    val libraryItem = libraryItem {
      media {
        metadata {
          seriesName = null
          seriesSequence = SeriesSequence(
            id = "",
            name = "test_series_name",
            sequence = 0,
          )
        }
      }
    }
    val seriesBooks = listOf(libraryItem())
    val slot = SeriesSlot(libraryItem, seriesBooks)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Series").isDisplayed()
    onNodeWithText("test_series_name").isDisplayed()
  }
}
