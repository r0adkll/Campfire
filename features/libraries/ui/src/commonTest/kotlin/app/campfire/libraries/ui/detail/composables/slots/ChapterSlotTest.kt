package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.home.ui.chapter
import app.campfire.home.ui.libraryItem
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class ChapterSlotTest {

  @Test
  fun showTimeInBookFalseTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val chapter = chapter(
      start = 1.minutes,
      end = 3.minutes,
    )
    val slot = ChapterSlot(
      libraryItem = libraryItem,
      chapter = chapter,
      showTimeInBook = false,
      mediaProgress = null,
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText(chapter.title).isDisplayed()
    onNodeWithText("2:00").isDisplayed()
  }

  @Test
  fun showTimeInBookTrueTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val chapter = chapter(
      start = 1.minutes,
      end = 3.minutes,
    )
    val slot = ChapterSlot(
      libraryItem = libraryItem,
      chapter = chapter,
      showTimeInBook = true,
      mediaProgress = null,
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText(chapter.title).isDisplayed()
    onNodeWithText("1:00").isDisplayed()
  }
}
