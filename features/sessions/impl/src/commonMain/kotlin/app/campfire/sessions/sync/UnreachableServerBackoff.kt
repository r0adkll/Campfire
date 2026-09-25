// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Stretches the playback sync cadence while the server can't be reached. A device can be
 * "connected" yet have no route to the server (off the home network, server down), and every
 * failed sync costs a radio wake-up plus a connect timeout. Listening sessions are persisted
 * locally until they upload, so waiting longer loses nothing.
 *
 * Each consecutive unreachable sync doubles the interval, capped at [cap]. The first sync that
 * reaches the server — or a change in the device's network — restores the normal cadence.
 */
internal class UnreachableServerBackoff(
  private val cap: Duration = DEFAULT_CAP,
) {

  private var failures = 0

  /** The sync interval to honor given the normal [base] interval. */
  fun interval(base: Duration): Duration {
    if (failures == 0) return base
    val stretched = base * (1 shl failures)
    return stretched.coerceIn(base, maxOf(base, cap))
  }

  fun onReachable() {
    failures = 0
  }

  fun onUnreachable() {
    failures = (failures + 1).coerceAtMost(MAX_DOUBLINGS)
  }

  private companion object {
    val DEFAULT_CAP = 5.minutes

    // Keeps `1 shl failures` from overflowing; the cap is reached long before this
    const val MAX_DOUBLINGS = 16
  }
}
