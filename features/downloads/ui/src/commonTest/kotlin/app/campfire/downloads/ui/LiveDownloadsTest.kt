// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import app.campfire.core.model.LibraryItemId
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloads
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class LiveDownloadsTest {

  private val book = DownloadContent.Audiobook("book")
  private val other = DownloadContent.Audiobook("other")

  private fun downloading(percent: Int) = Download(book, Download.State.Downloading, progress = percent / 100f)
  private val downloaded = Download(book, Download.State.Downloaded)
  private val otherDownloaded = Download(other, Download.State.Downloaded)

  /** Reports progress only to those following a download, as a platform's list of downloads may. */
  private class StateOnlyDownloads : Downloads {
    val all = MutableStateFlow<List<Download>>(emptyList())
    val live = MutableStateFlow<Map<DownloadContent, Download>>(emptyMap())
    val observed = mutableListOf<Set<DownloadContent>>()

    override fun observeAll(): Flow<List<Download>> = all
    override fun observe(contents: Set<DownloadContent>): Flow<Map<DownloadContent, Download>> {
      observed += contents
      return live.map { downloads -> downloads.filterKeys { it in contents } }
    }

    override fun download(contents: List<DownloadContent>) = Unit
    override fun stop(content: DownloadContent) = Unit
    override fun delete(content: DownloadContent) = Unit
    override suspend fun deleteAll(itemId: LibraryItemId) = Unit
  }

  private val downloads = StateOnlyDownloads()

  @Test
  fun aDownloadInProgress_showsItsProgressAsItGoes() = runTest {
    downloads.all.value = listOf(downloading(0), otherDownloaded)
    var latest = emptyList<Download>()
    val collecting = launch { downloads.observeAllWithProgress().collect { latest = it } }

    testScheduler.runCurrent()
    assertThat(latest).containsExactly(downloading(0), otherDownloaded)

    // The list of downloads says nothing, only the download's own progress moves
    downloads.live.value = mapOf(book to downloading(40))
    testScheduler.runCurrent()
    assertThat(latest).containsExactly(downloading(40), otherDownloaded)

    collecting.cancel()
    assertThat(downloads.observed).containsExactly(setOf<DownloadContent>(book))
  }

  @Test
  fun aFinishedDownload_isNoLongerFollowed_norShownAsItLastWas() = runTest {
    downloads.all.value = listOf(downloading(0))
    downloads.live.value = mapOf(book to downloading(90))
    var latest = emptyList<Download>()
    val collecting = launch { downloads.observeAllWithProgress().collect { latest = it } }
    testScheduler.runCurrent()

    downloads.all.value = listOf(downloaded)
    testScheduler.runCurrent()

    assertThat(latest).containsExactly(downloaded)
    collecting.cancel()
  }

  @Test
  fun withNothingInProgress_noDownloadIsFollowed() = runTest {
    downloads.all.value = listOf(otherDownloaded)
    var latest = emptyList<Download>()
    val collecting = launch { downloads.observeAllWithProgress().collect { latest = it } }
    testScheduler.runCurrent()
    collecting.cancel()

    assertThat(latest).containsExactly(otherDownloaded)
    assertThat(downloads.observed.size).isEqualTo(0)
  }
}
