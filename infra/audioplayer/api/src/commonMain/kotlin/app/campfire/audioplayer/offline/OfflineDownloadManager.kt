package app.campfire.audioplayer.offline

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import kotlinx.coroutines.flow.Flow

interface OfflineDownloadManager {

  /**
   * Observe all downloads
   * @return A [Flow] of a list of all [OfflineDownload]s.
   */
  fun observeAll(): Flow<List<OfflineDownload>>

  /**
   * Observe the current download status for a given item
   * @param item The [LibraryItem] to observe.
   * @return A [Flow] of the [OfflineDownload] for the given item.
   */
  fun observeForItem(item: LibraryItem): Flow<OfflineDownload>

  /**
   * Get the current download status for a given item
   * @param item The [LibraryItem] to get the download status for.
   * @return The [OfflineDownload] for the given item, or null if there is no download.
   */
  fun getForItem(item: LibraryItem): OfflineDownload

  /**
   * Observe the current download status for a list of items
   * @param items The list of [LibraryItem]s to observe.
   * @return A [Flow] of a map of [LibraryItemId] to [OfflineDownload] for the given items.
   */
  fun observeForItems(items: List<LibraryItem>): Flow<Map<LibraryItemId, OfflineDownload>>

  /**
   * Observe the current download status for a single podcast episode.
   */
  fun observeForEpisode(item: LibraryItem, episode: PodcastEpisode): Flow<OfflineDownload>

  /**
   * Observe the current download status for a list of podcast episodes within the
   * same parent [item].
   */
  fun observeForEpisodes(
    item: LibraryItem,
    episodes: List<PodcastEpisode>,
  ): Flow<Map<PodcastEpisodeId, OfflineDownload>>

  /**
   * Download a [LibraryItem] for offline playback.
   * @param item The [LibraryItem] to download.
   */
  fun download(item: LibraryItem)

  /**
   * Download several [LibraryItem]s for offline playback.
   * @param items The list of [LibraryItem] to download.
   */
  fun downloadAll(items: List<LibraryItem>)

  /**
   * Download a single podcast [episode] for offline playback.
   */
  fun downloadEpisode(item: LibraryItem, episode: PodcastEpisode)

  /**
   * Delete the offline download of a [LibraryItem].
   * @param item The [LibraryItem] to delete the download for.
   */
  fun delete(item: LibraryItem)

  /**
   * Delete the offline download of a single podcast [episode].
   */
  fun deleteEpisode(item: LibraryItem, episode: PodcastEpisode)

  /**
   * Stop any current download of a [LibraryItem].
   * @param item The [LibraryItem] to stop the download for.
   */
  fun stop(item: LibraryItem)

  /**
   * Stop any current download of a single podcast [episode].
   */
  fun stopEpisode(item: LibraryItem, episode: PodcastEpisode)

  /**
   * Resume any paused downloads due to process death or other events
   * where they might not automatically resume.
   */
  fun resumeDownloads()
}
