// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Whether the user is scrolling anything beneath where [nestedScrollConnection] is installed.
 *
 * Scrollable lists hand every drag and fling delta up through nested scroll before consuming it,
 * so one connection near the root sees them all. [isScrolling] turns true on the first delta and
 * false once none has arrived for [settleTime]. It only changes at those two moments, and lives
 * outside the snapshot system, so the cards waiting on it cost nothing per scroll frame.
 */
@Stable
class UserScrollTracker(
  private val scope: CoroutineScope,
  private val settleTime: Duration = DefaultSettleTime,
  private val timeSource: TimeSource = TimeSource.Monotonic,
) {
  private val scrolling = MutableStateFlow(false)

  val isScrolling: Boolean get() = scrolling.value

  private var lastScroll = timeSource.markNow()

  val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
      if (available != Offset.Zero) onScroll()
      return Offset.Zero
    }
  }

  internal fun onScroll() {
    lastScroll = timeSource.markNow()
    if (scrolling.value) return
    scrolling.value = true
    // One wait per gesture rather than one per frame: keep sleeping until a full settle time has
    // passed since the latest delta
    scope.launch {
      while (true) {
        val remaining = settleTime - lastScroll.elapsedNow()
        if (remaining <= Duration.ZERO) break
        delay(remaining)
      }
      scrolling.value = false
    }
  }

  /**
   * Suspends until the user hasn't scrolled for [duration] in a row: right away if they aren't
   * scrolling now, the wait starts over whenever a scroll begins during it.
   *
   * Timed on this tracker's own scope, so it keeps the same clock as [isScrolling] settling.
   */
  suspend fun awaitStillFor(duration: Duration) {
    withContext(scope.coroutineContext.minusKey(Job)) {
      while (true) {
        awaitNotScrolling()
        val interrupted = withTimeoutOrNull(duration) { scrolling.first { it } }
        if (interrupted == null) break
      }
    }
  }

  /** Suspends until no scroll is in progress, without asking for any frames meanwhile. */
  suspend fun awaitNotScrolling() {
    scrolling.first { !it }
  }

  /** Suspends until the next scroll has started and settled. */
  suspend fun awaitNextScroll() {
    scrolling.first { it }
    awaitNotScrolling()
  }

  companion object {
    val DefaultSettleTime: Duration = 200.milliseconds
  }
}

@Composable
fun rememberUserScrollTracker(): UserScrollTracker {
  val scope = rememberCoroutineScope()
  return remember(scope) { UserScrollTracker(scope) }
}

/** The tracker for the screen, or null where none is installed (and nothing reads as scrolling). */
val LocalUserScrollTracker = staticCompositionLocalOf<UserScrollTracker?> { null }
