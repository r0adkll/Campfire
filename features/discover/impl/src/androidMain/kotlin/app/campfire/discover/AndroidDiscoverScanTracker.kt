// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.discover.api.DiscoverScanState
import app.campfire.discover.api.DiscoverScanTracker
import com.r0adkll.kimchi.annotations.ContributesBinding
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

/**
 * [DiscoverScanTracker] backed by WorkManager: scans run as unique background
 * work ([DiscoverScanWorker]) that survives navigation, process death, and the
 * app being swiped away, resuming on its own after interruptions. State is
 * derived rather than owned — live progress from the unique work's WorkInfo,
 * completion metadata from [DiscoverScanStateStore].
 */
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class, replaces = [InProcessDiscoverScanTracker::class])
@Inject
class AndroidDiscoverScanTracker(
  application: Application,
  private val userSession: UserSession,
  private val store: DiscoverScanStateStore,
  @ForScope(UserScope::class) coroutineScopeHolder: CoroutineScopeHolder,
) : DiscoverScanTracker {

  private val workManager = WorkManager.getInstance(application)

  override val state: StateFlow<DiscoverScanState> = combine(
    workManager.getWorkInfosForUniqueWorkFlow(DiscoverScanWorker.UNIQUE_WORK_NAME),
    store.observeLastCompleted(),
  ) { infos, completed ->
    // An actively running worker beats persisted metadata. Work that's merely
    // enqueued (unmet constraint, retry backoff) falls through to it, so a
    // rate-limit pause reads as Completed(rateLimited) rather than Running —
    // and a system stop that persisted a fresh checkpoint self-corrects when
    // the re-enqueued work runs again.
    val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
    when {
      running != null -> DiscoverScanState.Running(
        done = running.progress.getInt(DiscoverScanWorker.KEY_DONE, 0),
        total = running.progress.getInt(DiscoverScanWorker.KEY_TOTAL, 0),
      )
      completed != null -> completed
      else -> DiscoverScanState.Idle
    }
  }.stateIn(
    scope = coroutineScopeHolder.get(),
    started = SharingStarted.Eagerly,
    initialValue = store.lastCompleted() ?: DiscoverScanState.Idle,
  )

  override fun startScan() {
    // An explicit scan is the user asking for current data: bypass the series
    // cache and refetch every listing from the provider.
    enqueue(refresh = true)
  }

  override fun startScanIfStale() {
    val completed = state.value as? DiscoverScanState.Completed
    if (completed == null || !store.isFresh(completed)) enqueue(refresh = false)
  }

  override fun cancelScan() {
    workManager.cancelUniqueWork(DiscoverScanWorker.UNIQUE_WORK_NAME)
  }

  private fun enqueue(refresh: Boolean) {
    val userId = userSession.userId ?: return
    val request = OneTimeWorkRequestBuilder<DiscoverScanWorker>()
      .setConstraints(
        Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build(),
      )
      .setInputData(
        workDataOf(
          DiscoverScanWorker.KEY_USER_ID to userId,
          DiscoverScanWorker.KEY_REFRESH to refresh,
        ),
      )
      .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
      .build()
    // KEEP: starting a scan while one runs (or waits on a constraint) is a
    // no-op, matching the in-process tracker's behavior.
    workManager.enqueueUniqueWork(DiscoverScanWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
  }
}
