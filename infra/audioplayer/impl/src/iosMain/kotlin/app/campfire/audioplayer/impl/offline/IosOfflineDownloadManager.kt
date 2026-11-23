// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import app.campfire.account.api.UserSessionManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.session.UserSession
import app.campfire.offline.DownloadConditions
import app.campfire.settings.api.MobileDataSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

/**
 * iOS offline downloads: the queue desktop uses ([OfflineDownloadQueue]), stored in Application
 * Support ([iosDownloadsRoot]) and fetched through a background `URLSession`
 * ([BackgroundSessionFileFetcher]), so a book keeps downloading while the app is in the background.
 *
 * iOS doesn't hold downloads to [DownloadConditions] itself, so the queue waits for them; while
 * the app is suspended, "Download on Wi-Fi only" is kept by not letting requests use mobile data.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<OfflineDownloadManager>())
@Inject
class IosOfflineDownloadManager(
  accountManager: AccountManager,
  tokenRefresher: TokenRefresher,
  userSessionManager: UserSessionManager,
  downloadConditions: DownloadConditions,
  mobileDataSettings: MobileDataSettings,
  @ForScope(AppScope::class) scope: CoroutineScope,
) : OfflineDownloadManager {

  private val root = iosDownloadsRoot().also(::prepareDownloadsRoot)

  private val queue = OfflineDownloadQueue(
    store = OfflineDownloadStore(root),
    fetcher = BackgroundSessionFileFetcher(
      accountManager = accountManager,
      tokenRefresher = tokenRefresher,
      root = root,
      allowsCellularAccess = { !mobileDataSettings.observeDownloadOnWifiOnly().value },
    ),
    currentUser = { (userSessionManager.current as? UserSession.LoggedIn)?.user },
    scope = scope,
    ioDispatcher = Dispatchers.IO,
    downloadsAllowed = downloadConditions.observeAllowed(),
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
}
