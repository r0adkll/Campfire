// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.api

import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

/**
 * Runs and observes the library-wide series scan behind the Upcoming screen.
 *
 * A scan walks every series in the user's library and asks the book info
 * registry for the provider's canonical listing. Everything fetched persists
 * in the registry's series cache — the Upcoming screen and the home screen's
 * upcoming shelf both read `BookInfoRegistry.observeCachedUpcoming`, so
 * results stream in live during a scan and survive restarts. The tracker only
 * carries progress and completion metadata; on Android scans run as background
 * work that outlives the app process, elsewhere they run in-process on the
 * user scope.
 */
interface DiscoverScanTracker {
  val state: StateFlow<DiscoverScanState>

  /**
   * Starts a scan that bypasses cached series listings and refetches every
   * series from the provider — an explicit scan means the user wants current
   * data, so it behaves like clearing the series cache (ratings and reviews
   * are untouched). A no-op while a scan is already running.
   */
  fun startScan()

  /**
   * Starts a scan only when there's nothing fresh to show — the tracker is
   * [DiscoverScanState.Idle], or the last completed scan has outlived its
   * freshness window. Fresh results are kept as-is; use [startScan]
   * (pull-to-refresh) to force one.
   */
  fun startScanIfStale()

  /** Stops a running scan; everything already cached stays visible. */
  fun cancelScan()
}

sealed interface DiscoverScanState {
  data object Idle : DiscoverScanState

  data class Running(
    val done: Int,
    val total: Int,
  ) : DiscoverScanState

  data class Completed(
    val scannedAt: Instant,
    /** Series that couldn't be looked up at all (no usable identifiers or provider). */
    val skippedCount: Int,
    /** Series whose provider fetch failed (network, server error). */
    val failedCount: Int,
    /**
     * The scan stopped early because the provider rate limited it. On Android
     * the work retries itself and clears this on a full pass; elsewhere the
     * next scan picks up from the cache checkpoint.
     */
    val rateLimited: Boolean = false,
  ) : DiscoverScanState
}
