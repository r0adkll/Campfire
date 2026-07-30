// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.test.offline

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeOfflineDownloadManager : OfflineDownloadManager {

  val invocations = mutableListOf<Invocation>()

  val observeAllFlow = MutableSharedFlow<List<OfflineDownload>>(replay = 1)
  override fun observeAll(): Flow<List<OfflineDownload>> {
    return observeAllFlow
  }

  val observeForItemFlow = MutableSharedFlow<OfflineDownload>(replay = 1)
  override fun observeForItem(item: LibraryItem): Flow<OfflineDownload> {
    return observeForItemFlow
  }

  var getForItem: OfflineDownload? = null
  override fun getForItem(item: LibraryItem): OfflineDownload {
    return getForItem!!
  }

  val observeForItemsFlow = MutableSharedFlow<Map<LibraryItemId, OfflineDownload>>(replay = 1)
  override fun observeForItems(items: List<LibraryItem>): Flow<Map<LibraryItemId, OfflineDownload>> {
    return observeForItemsFlow
  }

  val observeForEpisodeFlow = MutableSharedFlow<OfflineDownload>(replay = 1)
  override fun observeForEpisode(item: LibraryItem, episode: PodcastEpisode): Flow<OfflineDownload> {
    return observeForEpisodeFlow
  }

  val observeForEpisodesFlow = MutableSharedFlow<Map<PodcastEpisodeId, OfflineDownload>>(replay = 1)
  override fun observeForEpisodes(
    item: LibraryItem,
    episodes: List<PodcastEpisode>,
  ): Flow<Map<PodcastEpisodeId, OfflineDownload>> {
    return observeForEpisodesFlow
  }

  override fun download(item: LibraryItem) {
    invocations += Invocation.Download(item)
  }

  override fun downloadAll(items: List<LibraryItem>) {
    invocations += Invocation.DownloadAll(items)
  }

  override fun downloadEpisode(item: LibraryItem, episode: PodcastEpisode) {
    invocations += Invocation.DownloadEpisode(item, episode)
  }

  override fun delete(item: LibraryItem) {
    invocations += Invocation.Delete(item)
  }

  override fun deleteEpisode(item: LibraryItem, episode: PodcastEpisode) {
    invocations += Invocation.DeleteEpisode(item, episode)
  }

  override fun stop(item: LibraryItem) {
    invocations += Invocation.Stop(item)
  }

  override fun stopEpisode(item: LibraryItem, episode: PodcastEpisode) {
    invocations += Invocation.StopEpisode(item, episode)
  }

  override fun resumeDownloads() {
    invocations += Invocation.ResumeDownloads
  }

  sealed interface Invocation {
    data class Download(val item: LibraryItem) : Invocation
    data class DownloadAll(val items: List<LibraryItem>) : Invocation
    data class DownloadEpisode(val item: LibraryItem, val episode: PodcastEpisode) : Invocation
    data class Delete(val item: LibraryItem) : Invocation
    data class DeleteEpisode(val item: LibraryItem, val episode: PodcastEpisode) : Invocation
    data class Stop(val item: LibraryItem) : Invocation
    data class StopEpisode(val item: LibraryItem, val episode: PodcastEpisode) : Invocation
    object ResumeDownloads : Invocation
  }
}
