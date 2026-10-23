// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline

import app.campfire.core.offline.OfflineStatus
import app.campfire.offline.Download.State
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class DownloadTest {

  @Test
  fun `only a finished download is available`() {
    State.entries.forEach { state ->
      assertThat(Download(DownloadContent.Audiobook("book"), state).isAvailable)
        .isEqualTo(state == State.Downloaded)
    }
  }

  @Test
  fun `queued and downloading are in progress`() {
    assertThat(Download(DownloadContent.Audiobook("book"), State.Queued).isInProgress).isTrue()
    assertThat(Download(DownloadContent.Audiobook("book"), State.Downloading).isInProgress).isTrue()
    assertThat(Download(DownloadContent.Audiobook("book"), State.Failed).isInProgress).isFalse()
  }

  @Test
  fun `downloads group by item and kind`() {
    val audiobook = Download(DownloadContent.Audiobook("book"), State.Downloaded)
    val ebook = Download(DownloadContent.Ebook("book"), State.Downloading)
    val episode = Download(DownloadContent.Episode("podcast", "ep-1"), State.Queued)

    val byItem = listOf(audiobook, ebook, episode).byItem()

    assertThat(byItem).containsOnly(
      "book" to ItemDownloads("book", audiobook = audiobook, ebook = ebook),
      "podcast" to ItemDownloads("podcast", episodes = mapOf("ep-1" to episode)),
    )
  }

  @Test
  fun `an item shows its furthest along download`() {
    val downloads = ItemDownloads(
      itemId = "podcast",
      episodes = mapOf(
        "ep-1" to Download(DownloadContent.Episode("podcast", "ep-1"), State.Stopped),
        "ep-2" to Download(DownloadContent.Episode("podcast", "ep-2"), State.Failed),
        "ep-3" to Download(DownloadContent.Episode("podcast", "ep-3"), State.Queued),
      ),
    )

    assertThat(downloads.furthest?.state).isEqualTo(State.Queued)
    assertThat(ItemDownloads("empty").furthest).isNull()
  }

  @Test
  fun `downloaded beats everything else`() {
    val downloads = ItemDownloads(
      itemId = "book",
      audiobook = Download(DownloadContent.Audiobook("book"), State.Downloading),
      ebook = Download(DownloadContent.Ebook("book"), State.Downloaded),
    )

    assertThat(downloads.furthest?.content).isEqualTo(DownloadContent.Ebook("book"))
  }

  @Test
  fun `an item's status is its furthest download, an episode's is its own`() {
    val byItem = listOf(
      Download(DownloadContent.Episode("podcast", "ep-1"), State.Downloaded),
      Download(DownloadContent.Episode("podcast", "ep-2"), State.Downloading, progress = 0.5f),
    ).byItem()

    assertThat(byItem.statusOf("podcast")).isEqualTo(OfflineStatus.Available)
    assertThat(byItem.statusOf("podcast", "ep-2")).isEqualTo(OfflineStatus.Downloading(0.5f))
    assertThat(byItem.statusOf("podcast", "ep-3")).isEqualTo(OfflineStatus.None)
    assertThat(byItem.statusOf("unknown")).isEqualTo(OfflineStatus.None)
  }

  @Test
  fun `each state has a status`() {
    val content = DownloadContent.Ebook("book")
    assertThat(null.asOfflineStatus()).isEqualTo(OfflineStatus.None)
    assertThat(Download(content, State.Queued).asOfflineStatus()).isEqualTo(OfflineStatus.Queued)
    assertThat(Download(content, State.Downloading).asOfflineStatus()).isEqualTo(OfflineStatus.Downloading(0f))
    assertThat(Download(content, State.Downloaded).asOfflineStatus()).isEqualTo(OfflineStatus.Available)
    assertThat(Download(content, State.Stopped).asOfflineStatus()).isEqualTo(OfflineStatus.Failed)
    assertThat(Download(content, State.Failed).asOfflineStatus()).isEqualTo(OfflineStatus.Failed)
  }
}
