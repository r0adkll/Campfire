// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audible

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes requests and spaces them at least [minInterval] apart. Audible's
 * catalog API is anonymous with no published limit; ~2 requests per second
 * keeps an hours-long library scan from looking like a scraper.
 */
internal class RequestThrottle(
  private val minInterval: Duration = 500.milliseconds,
  private val timeSource: TimeSource = TimeSource.Monotonic,
) {
  private val mutex = Mutex()
  private var lastRequestAt: TimeMark? = null

  suspend fun <T> withThrottle(block: suspend () -> T): T = mutex.withLock {
    lastRequestAt?.let { last ->
      val wait = minInterval - last.elapsedNow()
      if (wait.isPositive()) delay(wait)
    }
    try {
      block()
    } finally {
      lastRequestAt = timeSource.markNow()
    }
  }
}
