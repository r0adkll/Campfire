package app.campfire.audioplayer.impl.offline

import android.app.Application
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.LibraryItem
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Inject

@OptIn(UnstableApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidOfflineDownloadManager(
  private val application: Application,
  private val downloadTracker: DownloadTracker,
) : OfflineDownloadManager {

  @kotlin.OptIn(ExperimentalCoroutinesApi::class)
  override fun observeForItem(item: LibraryItem): Flow<OfflineDownload> {
    return downloadTracker.observe()
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

  override fun download(item: LibraryItem) {
    item.media.tracks.forEach { track ->
      val request = DownloadRequest.Builder(track.metadata.filename, track.contentUrlWithToken.toUri())
        .build()

      DownloadService.sendAddDownload(
        application,
        CampfireDownloadService::class.java,
        request,
        true,
      )
    }
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

  override fun stop(item: LibraryItem) {
    delete(item)
  }
}
