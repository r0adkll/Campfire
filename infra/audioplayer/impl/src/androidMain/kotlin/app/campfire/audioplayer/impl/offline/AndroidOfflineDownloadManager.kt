// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import android.app.Application
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.audioplayer.offline.OfflineDownloadPayload
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Inject

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidOfflineDownloadManager(
  private val application: Application,
  private val downloadTracker: DownloadTracker,
) : OfflineDownloadManager {

  override fun observeAll(): Flow<List<OfflineDownload>> {
    return downloadTracker.observeDownloads()
  }

  override fun observeForItem(item: LibraryItem): Flow<OfflineDownload> {
    return downloadTracker.observeEvents()
      .flatMapLatest {
        channelFlow {
          var download = downloadTracker.getOfflineDownload(item)
          do {
            send(download)
            delay(1.seconds)
            download = downloadTracker.getOfflineDownload(item)
          } while (isActive && download.state != OfflineDownload.State.Completed)
        }
      }
  }

  override fun getForItem(item: LibraryItem): OfflineDownload {
    return downloadTracker.getOfflineDownload(item)
  }

  @kotlin.OptIn(ExperimentalCoroutinesApi::class)
  override fun observeForItems(items: List<LibraryItem>): Flow<Map<LibraryItemId, OfflineDownload>> {
    return downloadTracker.observeEvents()
      .flatMapLatest {
        channelFlow {
          var downloads = items.associate { item ->
            val download = downloadTracker.getOfflineDownload(item)
            item.id to download
          }
          send(downloads)

          // If any of the download values are not completed then wait 5 seconds and loop
          while (isActive && downloads.values.any { it.isActive }) {
            delay(5.seconds)
            downloads = items.associate { item ->
              val download = downloadTracker.getOfflineDownload(item)
              item.id to download
            }
            send(downloads)
          }
        }
      }
  }

  override fun observeForEpisode(item: LibraryItem, episode: PodcastEpisode): Flow<OfflineDownload> {
    return downloadTracker.observeEvents()
      .flatMapLatest {
        channelFlow {
          var download = downloadTracker.getOfflineDownload(item, episode)
          do {
            send(download)
            delay(1.seconds)
            download = downloadTracker.getOfflineDownload(item, episode)
          } while (isActive && download.state != OfflineDownload.State.Completed)
        }
      }
  }

  override fun observeForEpisodes(
    item: LibraryItem,
    episodes: List<PodcastEpisode>,
  ): Flow<Map<PodcastEpisodeId, OfflineDownload>> {
    return downloadTracker.observeEvents()
      .flatMapLatest {
        channelFlow {
          var downloads = episodes.associate { episode ->
            episode.id to downloadTracker.getOfflineDownload(item, episode)
          }
          send(downloads)

          while (isActive && downloads.values.any { it.isActive }) {
            delay(5.seconds)
            downloads = episodes.associate { episode ->
              episode.id to downloadTracker.getOfflineDownload(item, episode)
            }
            send(downloads)
          }
        }
      }
  }

  override fun download(item: LibraryItem) {
    val payload = OfflineDownloadPayload(
      libraryItemId = item.id,
      episodeId = null,
      title = item.media.metadata.title.orEmpty(),
      subtitle = item.media.metadata.authorName.orEmpty(),
    ).encode()

    item.media.tracks.forEach { track ->
      val request = DownloadRequest.Builder(track.metadata.filename, track.contentUrl.toUri())
        .setData(payload)
        .build()

      DownloadService.sendAddDownload(
        application,
        CampfireDownloadService::class.java,
        request,
        true,
      )
    }
  }

  override fun downloadAll(items: List<LibraryItem>) {
    items.forEach { download(it) }
  }

  override fun downloadEpisode(item: LibraryItem, episode: PodcastEpisode) {
    val track = episode.audioTrack
    if (track == null) {
      bark { "Cannot download episode ${episode.id}: missing audioTrack" }
      return
    }
    val payload = OfflineDownloadPayload(
      libraryItemId = item.id,
      episodeId = episode.id,
      title = episode.title,
      subtitle = item.media.metadata.title.orEmpty(),
    ).encode()
    val request = DownloadRequest.Builder(episodeDownloadId(item.id, episode.id), track.contentUrl.toUri())
      .setData(payload)
      .build()

    DownloadService.sendAddDownload(
      application,
      CampfireDownloadService::class.java,
      request,
      true,
    )
  }

  override fun delete(item: LibraryItem) {
    item.media.tracks.forEach { track ->
      DownloadService.sendRemoveDownload(
        application,
        CampfireDownloadService::class.java,
        track.metadata.filename,
        true,
      )
    }
  }

  override fun deleteEpisode(item: LibraryItem, episode: PodcastEpisode) {
    DownloadService.sendRemoveDownload(
      application,
      CampfireDownloadService::class.java,
      episodeDownloadId(item.id, episode.id),
      true,
    )
  }

  override fun stop(item: LibraryItem) {
    delete(item)
  }

  override fun stopEpisode(item: LibraryItem, episode: PodcastEpisode) {
    deleteEpisode(item, episode)
  }

  override fun resumeDownloads() {
    DownloadService.sendResumeDownloads(
      application,
      CampfireDownloadService::class.java,
      /*foreground=*/
      true,
    )
  }

  private fun episodeDownloadId(itemId: LibraryItemId, episodeId: PodcastEpisodeId): String =
    "$itemId:$episodeId"
}
