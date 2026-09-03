// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.api

import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

/**
 * Runs and observes the library-wide series scan behind the Upcoming screen.
 *
 * A scan walks every series in the user's library, asks the book info registry
 * for the provider's canonical listing, and accumulates the announced books
 * that haven't released. It runs in the user scope so it survives navigation,
 * and its results live in memory — closing the app resets to
 * [DiscoverScanState.Idle], while everything fetched persists in the registry's
 * series cache (feeding the home screen's cached upcoming shelf).
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

  /** Stops a running scan, freezing partial results into [DiscoverScanState.Completed]. */
  fun cancelScan()
}

/** Accumulated scan output; streams in while running and freezes on completion. */
data class DiscoverScanResults(
  /** Display name of the provider serving series data, once one has answered. */
  val providerName: String? = null,
  val upcoming: List<DiscoveredBook> = emptyList(),
)

sealed interface DiscoverScanState {
  data object Idle : DiscoverScanState

  data class Running(
    val done: Int,
    val total: Int,
    val results: DiscoverScanResults,
  ) : DiscoverScanState

  data class Completed(
    val results: DiscoverScanResults,
    val scannedAt: Instant,
    /** Series that couldn't be looked up at all (no usable identifiers or provider). */
    val skippedCount: Int,
    /** Series whose provider fetch failed (network, rate limit). */
    val failedCount: Int,
  ) : DiscoverScanState
}
