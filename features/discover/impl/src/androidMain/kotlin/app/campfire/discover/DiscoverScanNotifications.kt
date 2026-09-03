// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.campfire.core.navigation.DeepLinkKeys
import app.campfire.features.discover.impl.R

/**
 * Builds and posts the scan worker's notifications: the ongoing progress
 * notification that doubles as the foreground-service notification, and the
 * dismissible completion summary shown when a scan finishes in the background.
 * Both tap through to the Upcoming screen via [DeepLinkKeys.Upcoming].
 */
internal class DiscoverScanNotifications(context: Context) {

  private val appContext: Context = context.applicationContext
  private val notificationManager = NotificationManagerCompat.from(appContext)

  fun buildProgress(
    done: Int,
    total: Int,
    contentIntent: PendingIntent,
    cancelIntent: PendingIntent,
  ): Notification {
    ensureChannel()
    return NotificationCompat.Builder(appContext, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(appContext.getString(R.string.scan_notification_title))
      .setContentText(
        total.takeIf { it > 0 }?.let { appContext.getString(R.string.scan_notification_progress, done, it) },
      )
      .setContentIntent(contentIntent)
      .setProgress(total, done, total == 0)
      .setOngoing(true)
      .setShowWhen(false)
      .setOnlyAlertOnce(true)
      .addAction(0, appContext.getString(R.string.scan_notification_cancel), cancelIntent)
      .apply {
        if (Build.VERSION.SDK_INT >= 31) {
          setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
      }
      .build()
  }

  @SuppressLint("MissingPermission")
  fun notifyProgress(done: Int, total: Int, contentIntent: PendingIntent, cancelIntent: PendingIntent) {
    if (!notificationManager.areNotificationsEnabled()) return
    notificationManager.notify(PROGRESS_NOTIFICATION_ID, buildProgress(done, total, contentIntent, cancelIntent))
  }

  @SuppressLint("MissingPermission")
  fun notifyCompleted(upcomingCount: Int, contentIntent: PendingIntent) {
    if (!notificationManager.areNotificationsEnabled()) return
    ensureChannel()
    val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(
        appContext.resources.getQuantityString(
          R.plurals.scan_notification_completed,
          upcomingCount,
          upcomingCount,
        ),
      )
      .setContentIntent(contentIntent)
      .setAutoCancel(true)
      .build()
    notificationManager.notify(COMPLETED_NOTIFICATION_ID, notification)
  }

  fun cancelProgress() {
    notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
  }

  private fun ensureChannel() {
    if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
    val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
      .setName(appContext.getString(R.string.scan_notification_channel_name))
      .setVibrationEnabled(false)
      .build()
    notificationManager.createNotificationChannel(channel)
  }

  companion object {
    private const val CHANNEL_ID = "app.campfire.notifications.scan"

    // Playback holds 100 and downloads 101.
    const val PROGRESS_NOTIFICATION_ID = 102
    const val COMPLETED_NOTIFICATION_ID = 103
  }
}
