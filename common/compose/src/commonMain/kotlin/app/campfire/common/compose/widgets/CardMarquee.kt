// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.scroll.LocalUserScrollTracker
import kotlin.random.Random

/**
 * Scrolls a card's overflowing title or subtitle, once the card has sat still on screen for a
 * moment.
 *
 * A marquee redraws its card every frame while it runs, and a screen of cards marqueeing at once
 * kept the whole screen drawing at full frame rate. So it only starts after [CardMarqueeDwellMillis]
 * (staggered, so a row doesn't move in lockstep), and stops, back at the start of the text,
 * whenever the user scrolls: a card scrolled past never starts, and one that was running gives
 * way to the scroll. When the list settles, the wait starts over.
 */
@Composable
fun Modifier.cardMarquee(
  enabled: Boolean = LocalItemCardMarquee.current,
  velocity: Dp = DefaultCardMarqueeVelocity,
  dwellMillis: Int = rememberCardMarqueeDwellMillis(),
): Modifier {
  val scrolling = LocalUserScrollTracker.current?.isScrolling == true
  return if (enabled && !scrolling) {
    basicMarquee(velocity = velocity, initialDelayMillis = dwellMillis)
  } else {
    this
  }
}

/** A dwell for one card, so its title and subtitle start together. */
@Composable
fun rememberCardMarqueeDwellMillis(): Int = remember { CardMarqueeDwellMillis.random(Random) }

/** How long a card waits, still and on screen, before its text starts to scroll. */
internal val CardMarqueeDwellMillis = 2_000..3_000

private val DefaultCardMarqueeVelocity = 30.dp
