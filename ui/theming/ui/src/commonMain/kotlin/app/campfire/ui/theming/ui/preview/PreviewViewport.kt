// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** How a scene is drawn into the preview: scaled by [scale] from its top-left, then moved by [offset]. */
@Immutable
internal data class ZoomTarget(val scale: Float, val offset: Offset) {
  companion object {
    val None = ZoomTarget(1f, Offset.Zero)
  }
}

/**
 * The size a scene is laid out at in a preview of [viewport].
 *
 * Shown whole, it takes the preview's shape, at least [minSize], and scales down to fit. Zoomed in
 * on a part, it takes at least [focusSize] whatever the preview's shape, as a phone's screen
 * would, and the zoom shows the part of it that matters. In a short preview, such as the one over
 * the builder's controls, laying the scene out to the preview's shape would leave too little
 * height for Home's shelves to show their names and titles, and stretch parts like the search bar
 * too wide to zoom in on.
 */
internal fun canvasSize(viewport: Size, minSize: Size, focusSize: Size, focused: Boolean): Size {
  if (focused) {
    return Size(maxOf(viewport.width, focusSize.width), maxOf(viewport.height, focusSize.height))
  }
  val scale = minOf(1f, viewport.width / minSize.width, viewport.height / minSize.height)
  return Size(viewport.width / scale, viewport.height / scale)
}

/** The whole of a scene laid out at [canvas], as large as fits in [viewport] and centered. */
internal fun fitTarget(viewport: Size, canvas: Size): ZoomTarget {
  if (viewport.isEmpty() || canvas.isEmpty()) return ZoomTarget.None
  val scale = minOf(viewport.width / canvas.width, viewport.height / canvas.height)
  return ZoomTarget(
    scale = scale,
    offset = Offset(
      x = (viewport.width - canvas.width * scale) / 2,
      y = (viewport.height - canvas.height * scale) / 2,
    ),
  )
}

/**
 * Zooms a preview of [viewport] in on [part] of a scene laid out at [canvas], centering the part as
 * far as the scene's edges allow.
 *
 * Scales are of the scene's size in the app, so a part shows as large on a small phone as on a
 * tablet. The part shows whole with [padding] around it, up to [MaxZoom]. One too big to show
 * whole at [MinZoom] is cut instead, as shrinking it further would make it too small to judge. It
 * never zooms out past the whole scene.
 */
internal fun zoomTo(part: Rect, viewport: Size, canvas: Size, padding: Float): ZoomTarget {
  if (viewport.isEmpty() || canvas.isEmpty() || part.isEmpty) return fitTarget(viewport, canvas)
  val whole = minOf(viewport.width / canvas.width, viewport.height / canvas.height)
  val fitsPart = minOf(
    viewport.width / (part.width + padding * 2),
    viewport.height / (part.height + padding * 2),
  )
  val scale = fitsPart.coerceIn(maxOf(MinZoom, whole), maxOf(MaxZoom, whole))
  return ZoomTarget(
    scale = scale,
    offset = Offset(
      x = centeredOn(part.center.x * scale, length = canvas.width * scale, viewport = viewport.width),
      y = centeredOn(part.center.y * scale, length = canvas.height * scale, viewport = viewport.height),
    ),
  )
}

/**
 * Where content of [length] goes in [viewport] to put [point] in the middle, without showing past
 * the content's edges. Content shorter than the viewport is centered instead.
 */
private fun centeredOn(point: Float, length: Float, viewport: Float): Float = if (length <= viewport) {
  (viewport - length) / 2
} else {
  (viewport / 2 - point).coerceIn(viewport - length, 0f)
}

/** As small as a zoomed-in part gets, of its size in the app, before it's cut instead. */
internal const val MinZoom = 0.8f

/**
 * As far as the preview zooms in, of a part's size in the app: enough to judge a thin progress bar
 * or a tab's name, while keeping what's around the part in sight.
 */
internal const val MaxZoom = 1.5f

/**
 * Where a preview's zoom stands. At rest, the scene shows whole, laid out to the preview's shape
 * and scaled to fit as the preview resizes. Zoomed, it's laid out for zooming in (see
 * [canvasSize]) and [scale], [x] and [y] place it.
 */
internal class PreviewZoom {
  val scale = Animatable(1f)
  val x = Animatable(0f)
  val y = Animatable(0f)

  /**
   * Whether the scene is laid out for zooming in and placed by the zoom. It stays so until zooming
   * back out finishes, so the scene doesn't reflow under the zoom.
   */
  var zoomed by mutableStateOf(false)

  /** What the zoom last moved to show, to tell a new part apart from the same part resized. */
  var shown: Any? = null

  /**
   * Whether the next move animates. It stays set until a move finishes, so one cut short by a
   * resize, as when the builder's tray opens with the page that asked for it, carries on toward
   * the new target rather than jumping there. Moves that only follow a resize snap.
   */
  private var animating = false

  val target: ZoomTarget
    get() = ZoomTarget(scale.value, Offset(x.value, y.value))

  /** Makes the next move animate, as it shows something new. */
  fun animateNextMove() {
    animating = true
  }

  suspend fun snapTo(target: ZoomTarget) {
    scale.snapTo(target.scale)
    x.snapTo(target.offset.x)
    y.snapTo(target.offset.y)
  }

  /** Moves to [target], animating if [animateNextMove] asked for it since the last move finished. */
  suspend fun moveTo(target: ZoomTarget) {
    if (!animating) {
      snapTo(target)
      return
    }
    coroutineScope {
      val spec = tween<Float>(durationMillis = 350, easing = FastOutSlowInEasing)
      launch { scale.animateTo(target.scale, spec) }
      launch { x.animateTo(target.offset.x, spec) }
      launch { y.animateTo(target.offset.y, spec) }
    }
    animating = false
  }
}
