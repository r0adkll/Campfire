// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Whether the user is scrolling anything beneath where [nestedScrollConnection] is installed.
 *
 * Scrollable lists hand every drag and fling delta up through nested scroll before consuming it,
 * so one connection near the root sees them all. [isScrolling] turns true on the first delta and
 * false once none has arrived for [settleTime]. It only changes at those two moments, so reading
 * it costs nothing per scroll frame.
 */
@Stable
class UserScrollTracker(
  private val scope: CoroutineScope,
  private val settleTime: Duration = DefaultSettleTime,
  private val timeSource: TimeSource = TimeSource.Monotonic,
) {
  var isScrolling: Boolean by mutableStateOf(false)
    private set

  private var lastScroll = timeSource.markNow()

  val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
      if (available != Offset.Zero) onScroll()
      return Offset.Zero
    }
  }

  internal fun onScroll() {
    lastScroll = timeSource.markNow()
    if (isScrolling) return
    isScrolling = true
    // One wait per gesture rather than one per frame: keep sleeping until a full settle time has
    // passed since the latest delta
    scope.launch {
      while (true) {
        val remaining = settleTime - lastScroll.elapsedNow()
        if (remaining <= Duration.ZERO) break
        delay(remaining)
      }
      isScrolling = false
    }
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
