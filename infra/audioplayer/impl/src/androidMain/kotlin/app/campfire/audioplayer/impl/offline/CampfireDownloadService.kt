package app.campfire.audioplayer.impl.offline

import android.app.Notification
import android.app.PendingIntent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.Corked
import app.campfire.infra.audioplayer.impl.R
import com.r0adkll.kimchi.annotations.ContributesTo

@OptIn(UnstableApi::class)
@ContributesTo(AppScope::class)
interface CampfireDownloadServiceComponent {
  val downloadManager: DownloadManager
  val activityIntentProvider: ActivityIntentProvider
}

@OptIn(UnstableApi::class)
class CampfireDownloadService : DownloadService(
  NOTIFICATION_ID,
  DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
  CHANNEL_ID,
  R.string.download_notification_channel_name,
  R.string.download_notification_channel_description,
) {

  private val component by lazy {
    ComponentHolder.component<CampfireDownloadServiceComponent>()
  }

  private val downloadNotificationHelper by lazy {
    DownloadNotificationHelper(
      this,
      CHANNEL_ID,
    )
  }

  override fun getDownloadManager(): DownloadManager {
    return component.downloadManager
  }

  override fun getScheduler(): Scheduler {
    return PlatformScheduler(this, JOB_ID)
  }

  override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
    val message = if (downloads.size > 1) {
      getString(R.string.download_notification_message, downloads.size)
    } else if (downloads.size == 1) {
      downloads.first().request.toMediaItem().mediaMetadata.title?.toString()
    } else {
      null
    }

    return downloadNotificationHelper.buildProgressNotification(
      this,
      R.drawable.ic_notification,
      PendingIntent.getActivity(
        this,
        0,
        component.activityIntentProvider.provide(),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      ),
      message,
      downloads,
      notMetRequirements,
    )
  }

  override fun onCreate() {
    super.onCreate()
    ibark { "--> Creating CampfireDownloadService…" }
  }

  override fun onDestroy() {
    super.onDestroy()
    ibark { "<-- Destroying CampfireDownloadService…" }
  }

  companion object : Corked("CampfireDownloadService") {
    private const val CHANNEL_ID = "app.campfire.notifications.download"
    private const val NOTIFICATION_ID = 101
    private const val JOB_ID = 1
  }
}
