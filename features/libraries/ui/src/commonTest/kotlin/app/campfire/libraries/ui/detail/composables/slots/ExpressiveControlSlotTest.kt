// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.common.test.mediaProgress as readingProgress
import app.campfire.core.model.preview.mediaProgress
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.libraries.ui.detail.TestLibraryItemId
import app.campfire.libraries.ui.detail.composables.setCampfireContent
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
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
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
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
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
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
  fun ebookWithReadingProgressContinuesReading() = runComposeUiTest {
    val libraryItem = libraryItem(
      media = media(numTracks = 0, numAudioFiles = 0, durationInMillis = 0L, ebookFormat = "epub"),
    )
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = null,
      mediaProgress = readingProgress(libraryItemId = TestLibraryItemId, duration = 0f, ebookProgress = 0.4f),
      isCurrentSession = false,
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("button_play")
      .assertExists()
      .assertTextContains("Continue reading")
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
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
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
    val offlineDownload = Download(
      content = DownloadContent.Audiobook(TestLibraryItemId),
      state = Download.State.Downloading,
    )
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = offlineDownload,
      mediaProgress = null,
      isCurrentSession = false,
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("offline_status_title").assertExists()
    onNodeWithText("Downloading").assertIsDisplayed()
    onNodeWithTag("indeterminate_progress_bar").assertIsDisplayed()
    onNodeWithTag("button_download").assertDoesNotExist()
  }

  @Test
  fun hlsShowsPlayOptionsMenuInsteadOfDownloadButton() = runComposeUiTest {
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = null,
      mediaProgress = null,
      isCurrentSession = false,
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
      canStreamHls = true,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("button_download").assertExists()
    onNodeWithContentDescription("More options").assertExists()
    onNodeWithContentDescription("Download").assertDoesNotExist()
  }

  @Test
  fun hlsKeepsPlayOptionsMenuWhileDownloading() = runComposeUiTest {
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = downloadingOfflineDownload(),
      mediaProgress = null,
      isCurrentSession = false,
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
      canStreamHls = true,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithContentDescription("More options").assertExists()
    onNodeWithContentDescription("Download").assertDoesNotExist()
  }

  @Test
  fun hlsCurrentSessionWithoutDownloadShowsDownloadButton() = runComposeUiTest {
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = null,
      mediaProgress = null,
      isCurrentSession = true,
      isQueued = false,
      hasSession = true,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
      canStreamHls = true,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithContentDescription("More options").assertDoesNotExist()
    onNodeWithContentDescription("Download").assertExists()
  }

  @Test
  fun hlsCurrentSessionWhileDownloadingHidesTrailingButton() = runComposeUiTest {
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = downloadingOfflineDownload(),
      mediaProgress = null,
      isCurrentSession = true,
      isQueued = false,
      hasSession = true,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
      canStreamHls = true,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("button_download").assertDoesNotExist()
    onNodeWithContentDescription("More options").assertDoesNotExist()
    onNodeWithContentDescription("Download").assertDoesNotExist()
  }

  @Test
  fun hlsCurrentSessionWithCompletedDownloadHidesTrailingButton() = runComposeUiTest {
    val contentLength = 10L * 1024L * 1024L
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = Download(
        content = DownloadContent.Audiobook(TestLibraryItemId),
        state = Download.State.Downloaded,
        bytes = contentLength,
        totalBytes = contentLength,
      ),
      mediaProgress = null,
      isCurrentSession = true,
      isQueued = false,
      hasSession = true,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
      canStreamHls = true,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("button_download").assertDoesNotExist()
  }

  @Test
  fun withoutDownloadPermissionHidesDownloadButton() = runComposeUiTest {
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = null,
      mediaProgress = null,
      isCurrentSession = false,
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
      canDownload = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithTag("button_play").assertExists()
    onNodeWithTag("button_download").assertDoesNotExist()
  }

  @Test
  fun withoutDownloadPermissionHidesDownloadOptionInPlayOptionsMenu() = runComposeUiTest {
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = null,
      mediaProgress = null,
      isCurrentSession = false,
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
      canStreamHls = true,
      canDownload = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithContentDescription("More options").performClick()
    onNodeWithText("Stream").assertExists()
    onNodeWithText("Download").assertDoesNotExist()
  }

  @Test
  fun withoutDownloadPermissionFailedDownloadCanBeDeletedButNotRetried() = runComposeUiTest {
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem(),
      offlineDownload = Download(
        content = DownloadContent.Audiobook(TestLibraryItemId),
        state = Download.State.Failed,
      ),
      mediaProgress = null,
      isCurrentSession = false,
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
      showConfirmDownloadDialogSetting = false,
      canDownload = false,
    )

    setCampfireContent {
      slot.Content(Modifier) {}
    }

    onNodeWithText("Download failed").assertIsDisplayed()
    onNodeWithContentDescription("Delete download").assertIsDisplayed()
    onNodeWithContentDescription("Retry download").assertDoesNotExist()
  }

  private fun downloadingOfflineDownload() = Download(
    content = DownloadContent.Audiobook(TestLibraryItemId),
    state = Download.State.Downloading,
  )

  @Test
  fun determinateTest() = runComposeUiTest {
    val progressBytes = 1L * 1024L * 1024L // 1Mb
    val contentLength = 10L * 1024L * 1024L // 10Mb
    // ExpressiveControlSlot prefers libraryItem.media.sizeInBytes over the offline
    // download's contentLength when present, so align the two for this test.
    val libraryItem = libraryItem(media = media(sizeInBytes = contentLength))
    val offlineDownload = Download(
      content = DownloadContent.Audiobook(TestLibraryItemId),
      state = Download.State.Downloading,
      bytes = progressBytes,
      totalBytes = contentLength,
      progress = 0.1f,
    )
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = offlineDownload,
      mediaProgress = null,
      isCurrentSession = false,
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
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
    val offlineDownload = Download(
      content = DownloadContent.Audiobook(TestLibraryItemId),
      state = Download.State.Downloaded,
      bytes = contentLength,
      totalBytes = contentLength,
    )
    val slot = ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = offlineDownload,
      mediaProgress = null,
      isCurrentSession = false,
      isQueued = false,
      hasSession = false,
      addToPlaylistDialog = AddToPlaylistDialog.NoOp,
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
