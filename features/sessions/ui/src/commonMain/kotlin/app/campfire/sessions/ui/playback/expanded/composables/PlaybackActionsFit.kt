// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The fraction of its full width the cover must keep, with the stacked [PlaybackActions] shown,
 * before the panel switches to [CompactPlaybackActions]. 1f switches as soon as the cover shrinks
 * at all; lower values tolerate some shrinking first.
 */
internal const val CompactActionsCoverThreshold = 0.95f

/**
 * Decides between the stacked [PlaybackActions] and the single-row [CompactPlaybackActions] by how
 * wide the aspect-ratio cover would be with the stacked layout.
 *
 * The decision is always made against the stacked layout, whichever is showing: switching to the
 * compact row frees height and grows the cover back to full width, so judging the cover as it is
 * drawn would flip straight back to stacked on the next frame. The cover slot's height under the
 * stacked layout is the measured slot plus the height the shown actions take, minus the height the
 * stacked actions take.
 *
 * Nothing here is snapshot state. The answer is published as a [StateFlow] that the caller
 * collects, so it lands on the frame after the measurement rather than inside the layout pass:
 * the cover subcomposes ([androidx.compose.foundation.layout.BoxWithConstraints]) — in the
 * shared-element lookahead pass, no less — and snapshot state written from layout while that
 * composes is a concurrent change Compose rejects outright.
 */
@Stable
internal class PlaybackActionsFit {
  /** The space the cover is offered, before its aspect ratio squares it off. */
  private var coverSlot: IntSize = IntSize.Zero
  private var shownActionsHeight: Int = 0
  private var stackedActionsHeight: Int = 0

  private var threshold: Float = CompactActionsCoverThreshold
  private var frozen: Boolean = false

  private val _useCompact = MutableStateFlow(false)

  /** Whether [CompactPlaybackActions] should be shown. Read it through [rememberUseCompact]. */
  val useCompact: StateFlow<Boolean> = _useCompact.asStateFlow()

  /**
   * Measures the space the cover is given. Put it on a box that fills the cover's slot — the cover
   * itself is squared off by its aspect ratio, which is the thing being measured for.
   */
  val coverSlotModifier: Modifier = Modifier.onSizeChanged { size ->
    coverSlot = size
    revaluate()
  }

  /** Measures whichever actions layout is showing; [compact] says which one it is. */
  fun actionsModifier(compact: Boolean): Modifier = Modifier.onSizeChanged { size ->
    shownActionsHeight = size.height
    if (!compact) stackedActionsHeight = size.height
    revaluate()
  }

  internal fun configure(threshold: Float, frozen: Boolean) {
    val wasFrozen = this.frozen
    this.threshold = threshold
    this.frozen = frozen
    // A drag holds the layout it started with; decide again once the sheet is back at rest.
    if (wasFrozen && !frozen) revaluate()
  }

  private fun revaluate() {
    if (frozen) return
    val slot = coverSlot
    // Nothing to go on until the cover and the stacked actions have both been measured.
    if (slot.width <= 0 || stackedActionsHeight == 0) return

    val stackedSlotHeight = slot.height + shownActionsHeight - stackedActionsHeight
    val stackedCoverFill = stackedSlotHeight.coerceIn(0, slot.width).toFloat() / slot.width
    _useCompact.value = stackedCoverFill < threshold
  }
}

/**
 * Whether to show [CompactPlaybackActions], by [threshold] — see [CompactActionsCoverThreshold].
 *
 * While [frozen], the cover's measurements are ignored and the last answer is held — pass true
 * while the sheet is displaced by a drag, whose shrinking would otherwise swap the actions out
 * from under the user's finger.
 */
@Composable
internal fun PlaybackActionsFit.rememberUseCompact(
  threshold: Float,
  frozen: Boolean = false,
): Boolean {
  SideEffect { configure(threshold, frozen) }
  val flow = useCompact
  val useCompact by produceState(flow.value, flow) {
    flow.collect { next ->
      // On a frame boundary: the swap relayouts the cover, which subcomposes, and applying it
      // from inside a layout pass is the concurrent change Compose rejects.
      withFrameNanos { }
      value = next
    }
  }
  return useCompact
}
