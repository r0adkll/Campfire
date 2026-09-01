// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.api

import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

/**
 * Runs and observes the library-wide series scan behind the Discover screen.
 *
 * A scan walks every series in the user's library, asks the book info registry
 * for the provider's canonical listing, and accumulates the books the user
 * doesn't own. The scan is user-initiated only, runs in the user scope so it
 * survives navigation, and its results live in memory — closing the app resets
 * to [DiscoverScanState.Idle]. Rescans are cheap while provider rows are fresh
 * (the registry serves them from its cache).
 */
interface DiscoverScanTracker {
  val state: StateFlow<DiscoverScanState>

  /** Starts a scan; a no-op while one is already running. */
  fun startScan()

  /** Stops a running scan, freezing partial results into [DiscoverScanState.Completed]. */
  fun cancelScan()
}

/** Accumulated scan output; streams in while running and freezes on completion. */
data class DiscoverScanResults(
  /** Display name of the provider serving series data, once one has answered. */
  val providerName: String? = null,
  val missing: List<DiscoveredBook> = emptyList(),
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
