// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline.test

import app.campfire.core.model.LibraryItemId
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloads
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Keeps [downloads] as set, and records every change asked of it in [invocations]. */
class FakeDownloads : Downloads {

  val downloads = MutableStateFlow<List<Download>>(emptyList())
  val invocations = mutableListOf<Invocation>()

  override fun observeAll(): Flow<List<Download>> = downloads

  override fun observe(contents: Set<DownloadContent>): Flow<Map<DownloadContent, Download>> =
    downloads.map { all -> all.filter { it.content in contents }.associateBy { it.content } }

  override fun download(contents: List<DownloadContent>) {
    invocations += Invocation.Download(contents)
  }

  override fun stop(content: DownloadContent) {
    invocations += Invocation.Stop(content)
  }

  override fun delete(content: DownloadContent) {
    invocations += Invocation.Delete(content)
  }

  override suspend fun deleteAll(itemId: LibraryItemId) {
    invocations += Invocation.DeleteAll(itemId)
  }

  sealed interface Invocation {
    data class Download(val contents: List<DownloadContent>) : Invocation
    data class Stop(val content: DownloadContent) : Invocation
    data class Delete(val content: DownloadContent) : Invocation
    data class DeleteAll(val itemId: LibraryItemId) : Invocation
  }
}
