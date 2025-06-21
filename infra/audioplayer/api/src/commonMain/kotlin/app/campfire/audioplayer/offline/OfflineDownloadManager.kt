package app.campfire.audioplayer.offline

import app.campfire.core.model.LibraryItem
import kotlinx.coroutines.flow.Flow

interface OfflineDownloadManager {

  /**
   * Observe the current download status for a given item
   * @param item The [LibraryItem] to observe.
   * @return A [Flow] of the [OfflineDownload] for the given item.
   */
  fun observeForItem(item: LibraryItem): Flow<OfflineDownload>

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
