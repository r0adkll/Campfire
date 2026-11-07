// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.scroll.LocalUserScrollTracker
import app.campfire.common.compose.scroll.UserScrollTracker
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Scrolls a card's overflowing title or subtitle, once the card has sat still on screen for a
 * moment, and pauses it in place while the user scrolls.
 *
 * A marquee redraws its card every frame while it runs, and a screen of cards marqueeing at once
 * kept the whole screen drawing at full frame rate. So this waits for [dwellMillis] of stillness
 * before scrolling, and freezes (requesting no frames at all) whenever a list is being scrolled,
 * picking up where it left off once it settles. Otherwise it behaves like
 * [androidx.compose.foundation.basicMarquee]: [CardMarqueeIterations] passes, a pause between
 * them, a third of the width between the end of the text and its next start.
 *
 * Lists compose cards a little beyond what's on screen, so a card can run through its passes
 * half hidden at an edge before it's ever seen. Once done, a card starts over (after the same
 * dwell) whenever a scroll leaves it somewhere new.
 */
@Composable
fun Modifier.cardMarquee(
  enabled: Boolean = LocalItemCardMarquee.current,
  velocity: Dp = DefaultCardMarqueeVelocity,
  dwellMillis: Int = rememberCardMarqueeDwellMillis(),
): Modifier {
  if (!enabled) return this
  return this then CardMarqueeElement(
    velocity = velocity,
    dwellMillis = dwellMillis,
    tracker = LocalUserScrollTracker.current,
  )
}

/** A dwell for one card, so its title and subtitle start together. */
@Composable
fun rememberCardMarqueeDwellMillis(): Int = remember { CardMarqueeDwellMillis.random(Random) }

/** How long a card waits, still and on screen, before its text starts to scroll. */
internal val CardMarqueeDwellMillis = 2_000..3_000

internal const val CardMarqueeIterations = 3

/** The pause between passes, matching basicMarquee's. */
internal const val CardMarqueeRepeatDelayMillis = 1_200

private val DefaultCardMarqueeVelocity = 30.dp

private data class CardMarqueeElement(
  val velocity: Dp,
  val dwellMillis: Int,
  val tracker: UserScrollTracker?,
) : ModifierNodeElement<CardMarqueeNode>() {
  override fun create() = CardMarqueeNode(velocity, dwellMillis, tracker)

  override fun update(node: CardMarqueeNode) = node.update(velocity, dwellMillis, tracker)

  override fun InspectorInfo.inspectableProperties() {
    name = "cardMarquee"
    properties["velocity"] = velocity
    properties["dwellMillis"] = dwellMillis
  }
}

private class CardMarqueeNode(
  private var velocity: Dp,
  private var dwellMillis: Int,
  private var tracker: UserScrollTracker?,
) : Modifier.Node(),
  LayoutModifierNode,
  DrawModifierNode {

  override val shouldAutoInvalidate: Boolean = false

  private var contentWidth = 0
  private var containerWidth = 0
  private val spacing: Int get() = containerWidth / 3

  /** How far into the current pass the text has scrolled, in px. Read only when drawing. */
  private var offset = 0f

  private var animation: Job? = null

  private val overflows: Boolean get() = contentWidth > containerWidth

  private val paused: Boolean get() = tracker?.isScrolling == true

  fun update(velocity: Dp, dwellMillis: Int, tracker: UserScrollTracker?) {
    val restart = velocity != this.velocity || dwellMillis != this.dwellMillis || tracker !== this.tracker
    this.velocity = velocity
    this.dwellMillis = dwellMillis
    this.tracker = tracker
    if (restart) restart()
  }

  override fun onAttach() = restart()

  override fun onDetach() {
    // The node's scope is cancelled with it
    animation = null
    offset = 0f
  }

  override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
    // Measure the text at its full width, as a marquee must, then take no more than we're given
    val placeable = measurable.measure(constraints.copy(maxWidth = Constraints.Infinity))
    val width = constraints.constrainWidth(placeable.width)
    val height = constraints.constrainHeight(placeable.height)
    if (placeable.width != contentWidth || width != containerWidth) {
      contentWidth = placeable.width
      containerWidth = width
      restart()
    }
    return layout(width, height) {
      // Relative, so right-to-left text rests at its start on the right
      placeable.placeRelative(0, 0)
    }
  }

  override fun ContentDrawScope.draw() {
    if (!overflows || offset == 0f) {
      if (overflows) {
        clipRect { this@draw.drawContent() }
      } else {
        drawContent()
      }
      return
    }

    val direction = if (layoutDirection == LayoutDirection.Rtl) 1f else -1f
    val period = (contentWidth + spacing).toFloat()
    clipRect {
      translate(left = direction * offset) { this@draw.drawContent() }
      // The text's next start, following on from its end
      translate(left = direction * (offset - period)) { this@draw.drawContent() }
    }
  }

  private fun restart() {
    animation?.cancel()
    offset = 0f
    if (!isAttached) return
    invalidateDraw()
    if (!overflows) return
    animation = coroutineScope.launch {
      do {
        awaitStillFor(dwellMillis)
        repeat(CardMarqueeIterations) { pass ->
          if (pass > 0) awaitStillFor(CardMarqueeRepeatDelayMillis)
          scrollOnePass()
        }
      } while (awaitMovedByScroll())
    }
  }

  /**
   * Waits for a scroll that leaves this card somewhere else, where it hasn't been seen to scroll.
   * False if nothing tracks scrolling, so the card is done for good.
   */
  private suspend fun awaitMovedByScroll(): Boolean {
    val tracker = tracker ?: return false
    val restingAt = requireLayoutCoordinates().positionInRoot()
    do tracker.awaitNextScroll() while (requireLayoutCoordinates().positionInRoot() == restingAt)
    return true
  }

  /** Waits until the user hasn't scrolled for [millis] in a row. */
  private suspend fun awaitStillFor(millis: Int) {
    val tracker = tracker
    if (tracker != null) tracker.awaitStillFor(millis.milliseconds) else delay(millis.toLong())
  }

  private suspend fun scrollOnePass() {
    val distance = (contentWidth + spacing).toFloat()
    val pxPerNano = with(requireDensity()) { velocity.toPx() } / 1_000_000_000f
    var lastFrame = -1L
    while (offset < distance) {
      if (paused) {
        // Hold where the text is, asking for no frames until the scroll settles
        tracker?.awaitNotScrolling()
        lastFrame = -1L
        continue
      }
      withFrameNanos { now ->
        if (lastFrame >= 0) offset = (offset + (now - lastFrame) * pxPerNano).coerceAtMost(distance)
        lastFrame = now
      }
      invalidateDraw()
    }
    // A full pass ends where it began
    offset = 0f
    invalidateDraw()
  }
}
