// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.campfire.core.di.ComponentHolder
import app.campfire.core.lifecycle.AppLifecycleState
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.navigation.DeepLinkKeys
import app.campfire.core.session.userId
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Runs one full [SeriesScanner] pass as unique background work, so an
 * hours-long cold scan doesn't depend on the app staying alive. Results are
 * durable as they land (the registry's series cache); this worker only relays
 * progress — to WorkManager for [AndroidDiscoverScanTracker] and to the
 * ongoing notification that carries its foreground promotion.
 */
internal class DiscoverScanWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  private val notifications = DiscoverScanNotifications(appContext)

  override suspend fun getForegroundInfo(): ForegroundInfo {
    return ForegroundInfo(
      DiscoverScanNotifications.PROGRESS_NOTIFICATION_ID,
      notifications.buildProgress(done = 0, total = 0, contentIntent(), cancelIntent()),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
  }

  override suspend fun doWork(): Result {
    val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
    // Re-runs (process death, system stop, rate-limit retry) fast-forward
    // through the series cache instead of refetching thousands of listings.
    val refresh = inputData.getBoolean(KEY_REFRESH, false) && runAttemptCount == 0

    // The user graph is rebuilt on every session change; resolve it fresh per
    // run and silently abort when the restored session isn't the user this
    // scan was enqueued for.
    val userComponent = ComponentHolder.maybeComponent<DiscoverScanUserComponent>()
      ?: return Result.failure()
    if (userComponent.currentUserSession.userId != userId) return Result.failure()

    // Regular (non-expedited) work has to promote itself to a foreground
    // service; when the app is in a state where that's disallowed the scan
    // still runs, just without the keep-alive.
    try {
      setForeground(getForegroundInfo())
    } catch (e: IllegalStateException) {
      bark(LogPriority.WARN, throwable = e) { "Scan running without foreground promotion" }
    }

    var last = SeriesScanner.Progress(done = 0, total = 0, skippedCount = 0, failedCount = 0)
    var publishedDone = 0
    var publishedAt = TimeSource.Monotonic.markNow()
    try {
      userComponent.seriesScanner.scan(refresh).collect { progress ->
        last = progress
        val step = max(1, progress.total / 200)
        val publish = progress.done - publishedDone >= step ||
          publishedAt.elapsedNow() >= 1.seconds ||
          progress.done == progress.total
        if (publish) {
          publishedDone = progress.done
          publishedAt = TimeSource.Monotonic.markNow()
          setProgress(workDataOf(KEY_DONE to progress.done, KEY_TOTAL to progress.total))
          notifications.notifyProgress(progress.done, progress.total, contentIntent(), cancelIntent())
        }
      }
    } finally {
      // Persist terminal metadata for every exit — completion, user cancel,
      // logout cancel, and system stops (quota) alike freeze the checkpoint.
      withContext(NonCancellable) {
        userComponent.discoverScanStateStore.write(last)
        notifications.cancelProgress()
      }
    }

    if (last.rateLimited) {
      // Linear backoff re-runs the scan later; the refresh downgrade above
      // makes the re-run a cheap fast-forward to where this one stopped.
      return Result.retry()
    }

    // A scan that finishes while the app is away gets a dismissible summary;
    // in the foreground the screen already shows the result.
    val appComponent = ComponentHolder.component<DiscoverScanAppComponent>()
    if (appComponent.appLifecycleObserver.state.value == AppLifecycleState.Background) {
      val upcomingCount = userComponent.bookInfoRegistry.observeCachedUpcoming().first().size
      notifications.notifyCompleted(upcomingCount, contentIntent())
    }
    return Result.success()
  }

  private fun contentIntent(): PendingIntent {
    val intent = ComponentHolder.component<DiscoverScanAppComponent>()
      .activityIntentProvider.provide()
      .putExtra(DeepLinkKeys.Upcoming, true)
    return PendingIntent.getActivity(
      applicationContext,
      OPEN_UPCOMING_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun cancelIntent(): PendingIntent {
    return WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
  }

  companion object {
    const val UNIQUE_WORK_NAME = "discover-scan"
    const val KEY_USER_ID = "user_id"
    const val KEY_REFRESH = "refresh"
    const val KEY_DONE = "done"
    const val KEY_TOTAL = "total"

    private const val OPEN_UPCOMING_REQUEST_CODE = 1020
  }
}
