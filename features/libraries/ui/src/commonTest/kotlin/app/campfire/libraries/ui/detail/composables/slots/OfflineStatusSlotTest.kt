package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.libraries.ui.detail.TestLibraryItemId
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class OfflineStatusSlotTest {

  @Test
  fun indeterminateTest_collapsed() = runComposeUiTest {
    val offlineDownload = OfflineDownload(
      libraryItemId = TestLibraryItemId,
      state = OfflineDownload.State.Downloading,
      progress = OfflineDownload.Progress(0L, 0.1f, indeterminate = true),
    )
    val slot = OfflineStatusSlot(offlineDownload)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("offline_title_bar").performClick()
    onNodeWithText("Downloading").assertIsDisplayed()
    onNodeWithTag("indeterminate_progress_bar").assertDoesNotExist()
  }

  @Test
  fun indeterminateTest_expanded() = runComposeUiTest {
    val offlineDownload = OfflineDownload(
      libraryItemId = TestLibraryItemId,
      state = OfflineDownload.State.Downloading,
      progress = OfflineDownload.Progress(0L, 0.1f, indeterminate = true),
    )
    val slot = OfflineStatusSlot(offlineDownload)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("offline_title_bar").assertExists()
    onNodeWithText("Downloading").assertIsDisplayed()
    onNodeWithTag("indeterminate_progress_bar").assertIsDisplayed()
  }

  @Test
  fun determinateTest_collapsed() = runComposeUiTest {
    val progressBytes = 1L * 1024L * 1024L // 1Mb
    val contentLength = 10L * 1024L * 1024L // 10Mb
    val offlineDownload = OfflineDownload(
      libraryItemId = TestLibraryItemId,
      state = OfflineDownload.State.Downloading,
      contentLength = contentLength,
      progress = OfflineDownload.Progress(progressBytes, 0.1f, indeterminate = false),
    )
    val slot = OfflineStatusSlot(offlineDownload)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("offline_title_bar").performClick()
    onNodeWithText("Downloading").assertIsDisplayed()
    onNodeWithTag("determinate_progress_bar").assertDoesNotExist()
    onNodeWithText("1 MB").assertDoesNotExist()
    onNodeWithText("10 MB").assertDoesNotExist()
    onNodeWithText("Stop downloading").assertDoesNotExist()
  }

  @Test
  fun determinateTest_expanded() = runComposeUiTest {
    val progressBytes = 1L * 1024L * 1024L // 1Mb
    val contentLength = 10L * 1024L * 1024L // 10Mb
    val offlineDownload = OfflineDownload(
      libraryItemId = TestLibraryItemId,
      state = OfflineDownload.State.Downloading,
      contentLength = contentLength,
      progress = OfflineDownload.Progress(progressBytes, 0.1f, indeterminate = false),
    )
    val slot = OfflineStatusSlot(offlineDownload)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("offline_title_bar").assertExists()
    onNodeWithText("Downloading").assertIsDisplayed()
    onNodeWithTag("determinate_progress_bar").assertIsDisplayed()
    onNodeWithText("1.0 MB").assertIsDisplayed()
    onNodeWithText("10.0 MB").assertIsDisplayed()
    onNodeWithText("Stop downloading").assertIsDisplayed()
  }

  @Test
  fun completedTest_collapsed() = runComposeUiTest {
    val contentLength = 10L * 1024L * 1024L // 10Mb
    val offlineDownload = OfflineDownload(
      libraryItemId = TestLibraryItemId,
      state = OfflineDownload.State.Completed,
      contentLength = contentLength,
      progress = OfflineDownload.Progress(contentLength, 1f),
    )
    val slot = OfflineStatusSlot(offlineDownload)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("offline_title_bar").assertExists()
    onNodeWithText("Available for offline").assertIsDisplayed()
    onNodeWithText("10.0 MB").assertDoesNotExist()
    onNodeWithText("Delete").assertDoesNotExist()
  }

  @Test
  fun completedTest_expanded() = runComposeUiTest {
    val contentLength = 10L * 1024L * 1024L // 10Mb
    val offlineDownload = OfflineDownload(
      libraryItemId = TestLibraryItemId,
      state = OfflineDownload.State.Completed,
      contentLength = contentLength,
      progress = OfflineDownload.Progress(contentLength, 1f),
    )
    val slot = OfflineStatusSlot(offlineDownload)

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithTag("offline_title_bar").performClick()
    onNodeWithText("Available for offline").assertIsDisplayed()
    onNodeWithTag("determinate_progress_bar").assertDoesNotExist()
    onNodeWithTag("indeterminate_progress_bar").assertDoesNotExist()
    onNodeWithText("10.0 MB").assertIsDisplayed()
    onNodeWithText("Delete").assertIsDisplayed()
  }
}
