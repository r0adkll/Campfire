package app.campfire.audioplayer.test.offline

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
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

  override fun download(item: LibraryItem) {
    invocations += Invocation.Download(item)
  }

  override fun delete(item: LibraryItem) {
    invocations += Invocation.Delete(item)
  }

  override fun stop(item: LibraryItem) {
    invocations += Invocation.Stop(item)
  }

  override fun resumeDownloads() {
    invocations += Invocation.ResumeDownloads
  }

  sealed interface Invocation {
    data class Download(val item: LibraryItem) : Invocation
    data class Delete(val item: LibraryItem) : Invocation
    data class Stop(val item: LibraryItem) : Invocation
    object ResumeDownloads : Invocation
  }
}
