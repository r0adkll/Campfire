// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import app.campfire.account.api.UserSessionManager
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.session.UserSession
import app.campfire.network.di.DownloadClient
import com.r0adkll.kimchi.annotations.ContributesBinding
import io.ktor.client.HttpClient
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

/**
 * Desktop offline downloads, stored under the app's config directory next to its database. See
 * [OfflineDownloadQueue] for how downloads run and [OfflineDownloadStore] for the layout on disk.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = OfflineDownloadManager::class)
@Inject
class DesktopOfflineDownloadManager(
  accountManager: AccountManager,
  tokenRefresher: TokenRefresher,
  userSessionManager: UserSessionManager,
  @DownloadClient client: HttpClient,
  @ForScope(AppScope::class) scope: CoroutineScope,
) : OfflineDownloadManager {

  private val queue = OfflineDownloadQueue(
    store = OfflineDownloadStore(downloadsDirectory()),
    fetcher = HttpFileFetcher(client, accountManager, tokenRefresher),
    currentUser = { (userSessionManager.current as? UserSession.LoggedIn)?.user },
    scope = scope,
  )

  /** The absolute path of a fully downloaded copy of [track], or null when it should stream. */
  fun localPathFor(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?, track: AudioTrack): String? =
    queue.localPathFor(libraryItemId, episodeId, track)

  override fun observeAll(): Flow<List<OfflineDownload>> = queue.observeAll()

  override fun observeForItem(item: LibraryItem): Flow<OfflineDownload> = queue.observeForItem(item)

  override fun getForItem(item: LibraryItem): OfflineDownload = queue.getForItem(item)

  override fun observeForItems(items: List<LibraryItem>): Flow<Map<LibraryItemId, OfflineDownload>> =
    queue.observeForItems(items)

  override fun observeForEpisode(item: LibraryItem, episode: PodcastEpisode): Flow<OfflineDownload> =
    queue.observeForEpisode(item, episode)

  override fun observeForEpisodes(
    item: LibraryItem,
    episodes: List<PodcastEpisode>,
  ): Flow<Map<PodcastEpisodeId, OfflineDownload>> = queue.observeForEpisodes(item, episodes)

  override fun download(item: LibraryItem) = queue.download(item)

  override fun downloadAll(items: List<LibraryItem>) = queue.downloadAll(items)

  override fun downloadEpisode(item: LibraryItem, episode: PodcastEpisode) = queue.downloadEpisode(item, episode)

  override fun delete(item: LibraryItem) = queue.delete(item)

  override fun deleteEpisode(item: LibraryItem, episode: PodcastEpisode) = queue.deleteEpisode(item, episode)

  override suspend fun deleteAllForItemId(itemId: LibraryItemId) = queue.deleteAllForItemId(itemId)

  override fun stop(item: LibraryItem) = queue.stop(item)

  override fun stopEpisode(item: LibraryItem, episode: PodcastEpisode) = queue.stopEpisode(item, episode)

  override fun resumeDownloads() = queue.resumeDownloads()

  private companion object {
    fun downloadsDirectory(): File {
      val userRoot = System.getProperty("java.util.prefs.userRoot", System.getProperty("user.home"))
      return File(userRoot, ".config/Campfire/downloads")
    }
  }
}
