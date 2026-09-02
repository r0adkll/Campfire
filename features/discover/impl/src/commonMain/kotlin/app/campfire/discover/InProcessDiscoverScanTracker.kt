// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.discover.api.DiscoverScanState
import app.campfire.discover.api.DiscoverScanTracker
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * [DiscoverScanTracker] for platforms without background work (desktop, iOS):
 * runs [SeriesScanner] on the user scope so a scan survives navigation but not
 * the process. Terminal metadata persists through [DiscoverScanStateStore], so
 * a relaunch rehydrates the last [DiscoverScanState.Completed] — the freshness
 * window holds across restarts and everything the scan cached renders
 * immediately. On Android this binding is replaced by the WorkManager-backed
 * tracker.
 */
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class InProcessDiscoverScanTracker(
  private val seriesScanner: SeriesScanner,
  private val store: DiscoverScanStateStore,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : DiscoverScanTracker {

  private val _state = MutableStateFlow(store.lastCompleted() ?: DiscoverScanState.Idle)
  override val state: StateFlow<DiscoverScanState> = _state.asStateFlow()

  private var scanJob: Job? = null

  override fun startScan() {
    // An explicit scan is the user asking for current data: bypass the series
    // cache and refetch every listing from the provider.
    launchScan(refresh = true)
  }

  override fun startScanIfStale() {
    val completed = _state.value as? DiscoverScanState.Completed
    if (completed == null || !store.isFresh(completed)) launchScan(refresh = false)
  }

  override fun cancelScan() {
    scanJob?.cancel()
  }

  private fun launchScan(refresh: Boolean) {
    if (scanJob?.isActive == true) return
    scanJob = coroutineScopeHolder.get().launch { scan(refresh) }
  }

  private suspend fun scan(refresh: Boolean) {
    var last = SeriesScanner.Progress(done = 0, total = 0, skippedCount = 0, failedCount = 0)
    try {
      seriesScanner.scan(refresh).collect { progress ->
        last = progress
        _state.value = DiscoverScanState.Running(progress.done, progress.total)
      }
    } finally {
      // Runs for both completion and cancellation — a cancelled scan freezes
      // its counters, and everything it fetched is already in the series
      // cache. The write also refreshes the persisted freshness window.
      _state.value = store.write(last)
    }
  }
}
