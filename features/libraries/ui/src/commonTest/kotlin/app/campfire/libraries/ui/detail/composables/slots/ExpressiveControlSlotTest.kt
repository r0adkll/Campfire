package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.model.preview.mediaProgress
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.ui.detail.TestLibraryItemId
import app.campfire.libraries.ui.detail.composables.setCampfireContent
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class ExpressiveControlSlotTest {

  @Test
  fun baseContentTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = null,
      mediaProgress = null,
      isCurrentSession = false,
      showConfirmDownloadDialogSetting = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
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
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = null,
      mediaProgress = mediaProgress,
      isCurrentSession = false,
      showConfirmDownloadDialogSetting = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
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
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = null,
      mediaProgress = mediaProgress,
      isCurrentSession = false,
      showConfirmDownloadDialogSetting = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("button_play")
      .assertExists()
      .assertTextContains("Play")

    onNodeWithTag("button_discard_progress").assertDoesNotExist()
    onNodeWithTag("button_mark_finished").assertDoesNotExist()
    onNodeWithTag("button_mark_not_finished").assertExists()
  }

  @Test
  fun indeterminateTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val offlineDownload = OfflineDownload(
      libraryItemId = TestLibraryItemId,
      state = OfflineDownload.State.Downloading,
      progress = OfflineDownload.Progress(0L, 0.1f, indeterminate = true),
    )
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = offlineDownload,
      mediaProgress = null,
      isCurrentSession = false,
      showConfirmDownloadDialogSetting = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("offline_status_title").assertExists()
    onNodeWithText("Downloading").assertIsDisplayed()
    onNodeWithTag("indeterminate_progress_bar").assertIsDisplayed()
  }

  @Test
  fun determinateTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val progressBytes = 1L * 1024L * 1024L // 1Mb
    val contentLength = 10L * 1024L * 1024L // 10Mb
    val offlineDownload = OfflineDownload(
      libraryItemId = TestLibraryItemId,
      state = OfflineDownload.State.Downloading,
      contentLength = contentLength,
      progress = OfflineDownload.Progress(progressBytes, 0.1f, indeterminate = false),
    )
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = offlineDownload,
      mediaProgress = null,
      isCurrentSession = false,
      showConfirmDownloadDialogSetting = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("offline_status_title").assertExists()
    onNodeWithText("Downloading").assertIsDisplayed()
    onNodeWithTag("determinate_progress_bar").assertIsDisplayed()
    onNodeWithText("1.0 MB").assertIsDisplayed()
    onNodeWithText("10.0 MB").assertIsDisplayed()
    onNodeWithContentDescription("Stop downloading").assertIsDisplayed()
  }

  @Test
  fun completedTest() = runComposeUiTest {
    val libraryItem = libraryItem()
    val contentLength = 10L * 1024L * 1024L // 10Mb
    val offlineDownload = OfflineDownload(
      libraryItemId = TestLibraryItemId,
      state = OfflineDownload.State.Completed,
      contentLength = contentLength,
      progress = OfflineDownload.Progress(contentLength, 1f),
    )
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = offlineDownload,
      mediaProgress = null,
      isCurrentSession = false,
      showConfirmDownloadDialogSetting = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("offline_status_title").assertIsDisplayed()
    onNodeWithText("Available for offline").assertIsDisplayed()
    onNodeWithTag("determinate_progress_bar").assertDoesNotExist()
    onNodeWithTag("indeterminate_progress_bar").assertDoesNotExist()
    onNodeWithText("10.0 MB").assertIsDisplayed()
    onNodeWithText("Delete").assertIsDisplayed()
  }
}
