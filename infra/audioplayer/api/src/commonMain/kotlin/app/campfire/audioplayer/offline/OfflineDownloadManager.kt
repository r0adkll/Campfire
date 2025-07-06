package app.campfire.audioplayer.offline

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

interface OfflineDownloadManager {

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
   * Download a [LibraryItem] for offline playback.
   * @param item The [LibraryItem] to download.
   */
  fun download(item: LibraryItem)

  /**
   * Delete the offline download of a [LibraryItem].
   * @param item The [LibraryItem] to delete the download for.
   */
  fun delete(item: LibraryItem)

  /**
   * Stop any current download of a [LibraryItem].
   * @param item The [LibraryItem] to stop the download for.
   */
  fun stop(item: LibraryItem)
}
