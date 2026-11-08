// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation

import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DownloadsSummaryTest {

  @Test
  fun `no downloads shows nothing`() {
    assertEquals(DownloadsSummary(count = 0, downloading = false), emptyList<Download>().summary())
  }

  @Test
  fun `counts every download, whatever its state`() {
    val summary = Download.State.entries
      .filter { it != Download.State.Downloading }
      .mapIndexed { i, state -> download("item-$i", state) }
      .summary()

    assertEquals(DownloadsSummary(count = 4, downloading = false), summary)
  }

  @Test
  fun `a download fetching right now is in progress`() {
    val summary = listOf(
      download("a", Download.State.Downloaded),
      download("b", Download.State.Downloading),
    ).summary()

    assertEquals(DownloadsSummary(count = 2, downloading = true), summary)
  }

  @Test
  fun `a queued download alone isn't in progress`() {
    assertFalse(listOf(download("a", Download.State.Queued)).summary().downloading)
  }

  @Test
  fun `badge counts are capped`() {
    assertEquals("999", badgeCountLabel(999))
    assertEquals("999+", badgeCountLabel(1_000))
  }

  private fun download(itemId: String, state: Download.State) = Download(
    content = DownloadContent.Audiobook(itemId),
    state = state,
  )
}
