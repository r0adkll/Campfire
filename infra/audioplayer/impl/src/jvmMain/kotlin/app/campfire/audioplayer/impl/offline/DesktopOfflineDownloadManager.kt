package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DesktopOfflineDownloadManager : OfflineDownloadManager {
  override fun observeForItem(item: LibraryItem): Flow<OfflineDownload> {
    return emptyFlow()
  }

  override fun download(item: LibraryItem) {
    bark { "Not implemented yet!" }
  }

  override fun delete(item: LibraryItem) {
    bark { "Not implemented yet!" }
  }

  override fun stop(item: LibraryItem) {
    bark { "Not implemented yet!" }
  }
}
