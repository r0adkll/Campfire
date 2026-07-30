// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import android.app.Notification
import android.app.PendingIntent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
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

  private val downloadNotifications by lazy {
    CampfireDownloadNotifications(
      context = this,
      channelId = CHANNEL_ID,
    )
  }

  override fun getDownloadManager(): DownloadManager {
    return component.downloadManager
  }

  override fun getScheduler(): Scheduler {
    return PlatformScheduler(this, JOB_ID)
  }

  override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
    return downloadNotifications.buildProgressNotification(
      smallIcon = R.drawable.ic_notification,
      contentIntent = PendingIntent.getActivity(
        this,
        0,
        component.activityIntentProvider.provide(),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      ),
      downloads = downloads,
      notMetRequirements = notMetRequirements,
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
