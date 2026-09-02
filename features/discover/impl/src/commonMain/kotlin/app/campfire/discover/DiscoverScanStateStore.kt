// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.discover.api.DiscoverScanState
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getLongOrNullFlow
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * How long a completed scan counts as fresh for
 * [app.campfire.discover.api.DiscoverScanTracker.startScanIfStale] — matched
 * to the registry's series miss TTL, since a sooner rescan couldn't surface
 * anything new anyway.
 */
private val SCAN_FRESHNESS_TTL = 1.hours

/**
 * Persists the terminal metadata of the last completed scan per user. Scan
 * *results* are already durable in the registry's series cache; this covers
 * the scalars ([DiscoverScanState.Completed]) so the freshness window and the
 * completion summary survive process death on every platform.
 */
@OptIn(ExperimentalSettingsApi::class)
@SingleIn(UserScope::class)
@Inject
class DiscoverScanStateStore(
  private val settings: ObservableSettings,
  private val userSession: UserSession,
  private val fatherTime: FatherTime,
) {

  fun lastCompleted(): DiscoverScanState.Completed? {
    val scannedAt = settings.getLongOrNull(atKey()) ?: return null
    return DiscoverScanState.Completed(
      scannedAt = Instant.fromEpochMilliseconds(scannedAt),
      skippedCount = settings.getInt(skippedKey(), 0),
      failedCount = settings.getInt(failedKey(), 0),
      rateLimited = settings.getBoolean(rateLimitedKey(), false),
    )
  }

  /** Persists [progress] as the last completed scan, stamped now, and returns it. */
  fun write(progress: SeriesScanner.Progress): DiscoverScanState.Completed {
    settings.putInt(skippedKey(), progress.skippedCount)
    settings.putInt(failedKey(), progress.failedCount)
    settings.putBoolean(rateLimitedKey(), progress.rateLimited)
    // Written last: the observe flow keys off this value, so the other
    // scalars are already in place when observers re-read.
    settings.putLong(atKey(), fatherTime.nowInEpochMillis())
    return checkNotNull(lastCompleted())
  }

  fun observeLastCompleted(): Flow<DiscoverScanState.Completed?> {
    return settings.getLongOrNullFlow(atKey()).map { lastCompleted() }
  }

  fun isFresh(completed: DiscoverScanState.Completed): Boolean {
    return fatherTime.nowInEpochMillis() - completed.scannedAt.toEpochMilliseconds() <
      SCAN_FRESHNESS_TTL.inWholeMilliseconds
  }

  private fun atKey() = "discover_scan_at_${userSession.userId.orEmpty()}"
  private fun skippedKey() = "discover_scan_skipped_${userSession.userId.orEmpty()}"
  private fun failedKey() = "discover_scan_failed_${userSession.userId.orEmpty()}"
  private fun rateLimitedKey() = "discover_scan_rate_limited_${userSession.userId.orEmpty()}"
}
