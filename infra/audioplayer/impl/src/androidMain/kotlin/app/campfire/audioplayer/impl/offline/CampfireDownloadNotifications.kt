// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.R as Media3R
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.scheduler.Requirements
import app.campfire.audioplayer.offline.OfflineDownloadPayload
import app.campfire.infra.audioplayer.impl.R

/**
 * Forked from `androidx.media3.exoplayer.offline.DownloadNotificationHelper` so the
 * foreground notification can show the title of the item currently downloading
 * (decoded from each request's [OfflineDownloadPayload]) and surface the library-item
 * count in the notification title when more than one is in flight.
 *
 * Aggregation logic (progress, paused-for-network detection, indeterminate progress)
 * matches the upstream helper so the notification still reflects Media3's view of
 * the world; only the presentation differs.
 */
@OptIn(UnstableApi::class)
class CampfireDownloadNotifications(
  context: Context,
  private val channelId: String,
) {

  private val appContext: Context = context.applicationContext

  fun buildProgressNotification(
    @DrawableRes smallIcon: Int,
    contentIntent: PendingIntent?,
    downloads: List<Download>,
    @Requirements.RequirementFlags notMetRequirements: Int,
  ): Notification {
    var totalPercentage = 0f
    var downloadTaskCount = 0
    var allDownloadPercentagesUnknown = true
    var haveDownloadedBytes = false
    var haveDownloadingTasks = false
    var haveQueuedTasks = false
    var haveRemovingTasks = false

    downloads.forEach { download ->
      when (download.state) {
        Download.STATE_REMOVING -> haveRemovingTasks = true
        Download.STATE_QUEUED -> haveQueuedTasks = true
        Download.STATE_RESTARTING,
        Download.STATE_DOWNLOADING,
        -> {
          haveDownloadingTasks = true
          val percent = download.percentDownloaded
          if (percent != C.PERCENTAGE_UNSET.toFloat()) {
            allDownloadPercentagesUnknown = false
            totalPercentage += percent
          }
          if (download.bytesDownloaded > 0L) haveDownloadedBytes = true
          downloadTaskCount++
        }
        else -> Unit
      }
    }

    @StringRes var titleStringId: Int = NULL_STRING_ID
    var showProgress = true
    when {
      haveDownloadingTasks -> titleStringId = Media3R.string.exo_download_downloading
      haveQueuedTasks && notMetRequirements != 0 -> {
        showProgress = false
        titleStringId = when {
          (notMetRequirements and Requirements.NETWORK_UNMETERED) != 0 ->
            Media3R.string.exo_download_paused_for_wifi
          (notMetRequirements and Requirements.NETWORK) != 0 ->
            Media3R.string.exo_download_paused_for_network
          else -> Media3R.string.exo_download_paused
        }
      }
      haveRemovingTasks -> titleStringId = Media3R.string.exo_download_removing
    }

    var maxProgress = 0
    var currentProgress = 0
    var indeterminateProgress = false
    if (showProgress) {
      maxProgress = 100
      if (haveDownloadingTasks) {
        currentProgress = if (downloadTaskCount > 0) (totalPercentage / downloadTaskCount).toInt() else 0
        indeterminateProgress = allDownloadPercentagesUnknown && haveDownloadedBytes
      } else {
        indeterminateProgress = true
      }
    }

    // Decode every download's payload once, group by libraryItemId so a multi-track
    // book counts as one logical item. Preserve insertion order via LinkedHashMap.
    val groupsByItem = linkedMapOf<String, MutableList<PayloadDownload>>()
    downloads.forEach { download ->
      val payload = OfflineDownloadPayload.decode(download.request.data)
      groupsByItem.getOrPut(payload.key.libraryItemId) { mutableListOf() }
        .add(PayloadDownload(download, payload))
    }

    val totalItems = groupsByItem.size
    val doneItems = groupsByItem.count { (_, group) ->
      group.all { it.download.state == Download.STATE_COMPLETED }
    }
    val inFlightItems = totalItems - doneItems

    // Lead: prefer the first in-flight group whose payload has any actively downloading
    // track so the notification follows what's currently being written to disk rather
    // than pinning to a queued item that hasn't started.
    val inFlightGroups = groupsByItem.values.filter { group -> group.any { it.isInFlight() } }
    val activeGroup = inFlightGroups.firstOrNull { group -> group.any { it.isActivelyDownloading() } }
    val leadPayload = (activeGroup ?: inFlightGroups.firstOrNull())?.first()?.payload
    val collapsedText = leadPayload?.let(::formatLine)

    val baseLabel = if (titleStringId == NULL_STRING_ID) null else appContext.getString(titleStringId)
    val contentTitle = when {
      baseLabel == null -> null
      inFlightItems > 1 -> appContext.getString(
        R.string.download_notification_title_with_count,
        baseLabel,
        inFlightItems,
      )
      else -> baseLabel
    }

    val subText = if (inFlightItems >= 1) {
      appContext.resources.getQuantityString(
        R.plurals.download_notification_items,
        inFlightItems,
        inFlightItems,
      )
    } else {
      null
    }

    val summaryText = if (showProgress && haveDownloadingTasks && !indeterminateProgress && totalItems > 1) {
      appContext.getString(
        R.string.download_notification_summary,
        currentProgress,
        doneItems,
        totalItems,
      )
    } else {
      null
    }

    val style = collapsedText?.let {
      NotificationCompat.BigTextStyle()
        .bigText(it)
        .also { style -> summaryText?.let(style::setSummaryText) }
    }

    val builder = NotificationCompat.Builder(appContext, channelId)
      .setSmallIcon(smallIcon)
      .setContentTitle(contentTitle)
      .setContentIntent(contentIntent)
      .setContentText(collapsedText)
      .setSubText(subText)
      .setNumber(inFlightItems)
      .setOngoing(true)
      .setShowWhen(false)
      .setOnlyAlertOnce(true)
      .setProgress(maxProgress, currentProgress, indeterminateProgress)

    style?.let(builder::setStyle)

    if (Build.VERSION.SDK_INT >= 31) {
      builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }

    return builder.build()
  }

  private fun formatLine(payload: OfflineDownloadPayload): String? {
    val title = payload.title.takeIf { it.isNotEmpty() } ?: return null
    return if (payload.subtitle.isNotEmpty()) "$title · ${payload.subtitle}" else title
  }

  private data class PayloadDownload(
    val download: Download,
    val payload: OfflineDownloadPayload,
  ) {
    fun isInFlight(): Boolean =
      download.state != Download.STATE_COMPLETED && download.state != Download.STATE_FAILED

    fun isActivelyDownloading(): Boolean = download.state == Download.STATE_DOWNLOADING
  }

  companion object {
    private const val NULL_STRING_ID = 0
  }
}
