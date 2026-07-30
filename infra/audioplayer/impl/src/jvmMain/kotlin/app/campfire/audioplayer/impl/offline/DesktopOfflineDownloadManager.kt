// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DesktopOfflineDownloadManager : OfflineDownloadManager {
  override fun observeAll(): Flow<List<OfflineDownload>> {
    return emptyFlow()
  }

  override fun observeForItem(item: LibraryItem): Flow<OfflineDownload> {
    return emptyFlow()
  }

  override fun getForItem(item: LibraryItem): OfflineDownload {
    return OfflineDownload(item.id)
  }

  override fun observeForItems(items: List<LibraryItem>): Flow<Map<LibraryItemId, OfflineDownload>> {
    return emptyFlow()
  }

  override fun observeForEpisode(item: LibraryItem, episode: PodcastEpisode): Flow<OfflineDownload> {
    return emptyFlow()
  }

  override fun observeForEpisodes(
    item: LibraryItem,
    episodes: List<PodcastEpisode>,
  ): Flow<Map<PodcastEpisodeId, OfflineDownload>> {
    return emptyFlow()
  }

  override fun download(item: LibraryItem) {
    bark { "Not implemented yet!" }
  }

  override fun downloadAll(items: List<LibraryItem>) {
    bark { "Not implemented yet!" }
  }

  override fun downloadEpisode(item: LibraryItem, episode: PodcastEpisode) {
    bark { "Not implemented yet!" }
  }

  override fun delete(item: LibraryItem) {
    bark { "Not implemented yet!" }
  }

  override fun deleteEpisode(item: LibraryItem, episode: PodcastEpisode) {
    bark { "Not implemented yet!" }
  }

  override fun stop(item: LibraryItem) {
    bark { "Not implemented yet!" }
  }

  override fun stopEpisode(item: LibraryItem, episode: PodcastEpisode) {
    bark { "Not implemented yet!" }
  }

  override fun resumeDownloads() {
    bark { "Not implemented yet!" }
  }
}
