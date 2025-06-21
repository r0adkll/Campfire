package app.campfire.audioplayer.impl.offline

import android.app.Application
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadService
import app.campfire.audioplayer.impl.asPlatformMediaItem
import app.campfire.audioplayer.impl.mediaitem.MediaItemBuilder
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import com.r0adkll.kimchi.annotations.ContributesBinding
import java.io.IOException
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
    val mediaItems = MediaItemBuilder.build(item)
      .map { it.asPlatformMediaItem() }

    mediaItems.forEach { mediaItem ->
      val helper = DownloadHelper.forMediaItem(application, mediaItem)
      helper.prepare(MediaItemDownloadHelperCallback(mediaItem))
    }
  }

  override fun delete(item: LibraryItem) {
    val mediaItems = MediaItemBuilder.build(item)
      .map { it.asPlatformMediaItem() }

    mediaItems.forEach { mediaItem ->
      DownloadService.sendRemoveDownload(
        application,
        CampfireDownloadService::class.java,
        mediaItem.localConfiguration!!.uri.toString(),
        true,
      )
    }
  }

  override fun stop(item: LibraryItem) {
    delete(item)
  }

  inner class MediaItemDownloadHelperCallback(
    private val mediaItem: MediaItem,
  ) : DownloadHelper.Callback {

    override fun onPrepared(helper: DownloadHelper) {
      val request = helper.getDownloadRequest(mediaItem.mediaId.toByteArray())
      DownloadService.sendAddDownload(
        application,
        CampfireDownloadService::class.java,
        request,
        true,
      )
      helper.release()
    }

    override fun onPrepareError(helper: DownloadHelper, e: IOException) {
      // TODO: Register this error in this class in a way that is observable
      //  so that listening UIs can give feedback to the user about what downloaded
      //  and what failed.
      bark(LogPriority.ERROR, throwable = e) {
        "Something went wrong when trying to prepare ${mediaItem.mediaId} for download"
      }

      // Temporary?
      Toast.makeText(application, "Failed to start download [${mediaItem.mediaId}]", Toast.LENGTH_LONG).show()

      helper.release()
    }
  }
}
