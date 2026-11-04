// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import kotlin.math.roundToInt

/**
 * How the parts of a [ThemePreviewContent] respond: which ones get a warning outline because
 * their contrast check fails, and what happens when one is tapped.
 */
@Immutable
internal data class ThemeTargets(
  val failing: Set<ContrastCheck> = emptySet(),
  val onClick: ((ContrastCheck) -> Unit)? = null,
) {
  companion object {
    val None = ThemeTargets()
  }
}

/**
 * Marks this element as the part of the preview that [check] covers. Place it before the
 * element's background so the warning outline draws on top.
 */
internal fun Modifier.themeTarget(
  check: ContrastCheck,
  targets: ThemeTargets,
  cornerRadius: Dp = 0.dp,
): Modifier {
  val outlined = if (check in targets.failing) {
    drawWithContent {
      drawContent()
      drawRoundRect(
        color = WarningOutlineColor,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
          width = 2.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        ),
      )
    }
  } else {
    this
  }

  return if (targets.onClick != null) outlined then PreviewTapElement(check = check, onClick = null) else outlined
}

/**
 * Keeps [onClick] working while the preview's parts can be tapped, for controls in the preview
 * that do something real, such as a link to settings. Without it the tap would pick the part
 * around the control instead.
 */
internal fun Modifier.previewAction(onClick: () -> Unit): Modifier =
  this then PreviewTapElement(check = null, onClick = onClick)

/**
 * Lets the parts of [content] be tapped when [targets] has a click handler. The preview's real
 * components handle taps of their own (cards, switches, text fields), and a part often contains
 * one, so rather than each part listening, a layer over the whole preview takes every tap and
 * gives it to the smallest part under it. That also keeps the preview's controls from reacting.
 *
 * The scene shows whole, laid out at least [minSize] and scaled down to fit (see [ScaledToFit]).
 * With a [focus], it's laid out with at least [focusSize] of room and zooms in on the part [focus]
 * checks, so the part shows at a size the user can judge, then zooms back out when [focus] clears.
 * [contentKey] names what [content] shows, so a new scene zooms to its own part.
 */
@Composable
internal fun TappablePreviewParts(
  targets: ThemeTargets,
  actionLabel: @Composable (ContrastCheck) -> String,
  minSize: DpSize,
  focusSize: DpSize,
  modifier: Modifier = Modifier,
  focus: ContrastCheck? = null,
  contentKey: Any? = null,
  content: @Composable () -> Unit,
) {
  val onClick = targets.onClick
  if (onClick == null) {
    ScaledToFit(minSize = minSize, modifier = modifier, content = content)
    return
  }

  val currentOnClick by rememberUpdatedState(onClick)
  val labels = ContrastCheck.entries.associateWith { actionLabel(it) }
  val parts = remember(labels) { PreviewParts(labels) { currentOnClick(it) } }
  val zoom = remember { PreviewZoom() }
  var size by remember { mutableStateOf(IntSize.Zero) }
  val density = LocalDensity.current
  val padding = with(density) { ZoomPadding.toPx() }
  val minSizePx = with(density) { minSize.toSize() }
  val focusSizePx = with(density) { focusSize.toSize() }

  LaunchedEffect(parts, focus, contentKey, size, minSizePx, focusSizePx) {
    if (size == IntSize.Zero) return@LaunchedEffect
    val viewport = size.toSize()
    val zoomedCanvas = canvasSize(viewport, minSizePx, focusSizePx, focused = true)
    val shown = focus to contentKey
    if (shown != zoom.shown) {
      zoom.shown = shown
      zoom.animateNextMove()
    }

    if (focus != null) {
      if (!zoom.zoomed) {
        // Lay the scene out with room for the part, showing it whole to zoom in from.
        zoom.snapTo(fitTarget(viewport, zoomedCanvas))
        zoom.zoomed = true
        withFrameNanos { }
      }
      // Wait for the parts to lay out at this size before reading where the focused one is.
      withFrameNanos { }
      val bounds = parts.boundsOf(focus)
      zoom.moveTo(
        if (bounds == null) fitTarget(viewport, zoomedCanvas) else zoomTo(bounds, viewport, zoomedCanvas, padding),
      )
    } else if (zoom.zoomed) {
      zoom.moveTo(fitTarget(viewport, zoomedCanvas))
      // Back to the preview's own shape, scaled to fit, once zoomed out.
      zoom.zoomed = false
    }
  }

  CompositionLocalProvider(LocalPreviewParts provides parts) {
    Layout(
      content = {
        // Taps and part bounds are both measured inside the scene, so neither needs the zoom undoing.
        Box {
          content()
          Box(
            Modifier
              .matchParentSize()
              .onPlaced { parts.layer = it }
              .pointerHoverIcon(PointerIcon.Hand)
              .pointerInput(parts) {
                detectTapGestures { position -> parts.tap(position) }
              },
          )
        }
      },
      modifier = modifier
        .clipToBounds()
        .onSizeChanged { size = it },
    ) { measurables, constraints ->
      val width = if (constraints.hasBoundedWidth) constraints.maxWidth else minSize.width.roundToPx()
      val height = if (constraints.hasBoundedHeight) constraints.maxHeight else minSize.height.roundToPx()
      val viewport = Size(width.toFloat(), height.toFloat())
      val canvas = canvasSize(viewport, minSizePx, focusSizePx, focused = zoom.zoomed)
      val placeable = measurables.single().measure(
        Constraints.fixed(canvas.width.roundToInt(), canvas.height.roundToInt()),
      )
      val laidOut = Size(placeable.width.toFloat(), placeable.height.toFloat())
      layout(width, height) {
        placeable.placeWithLayer(0, 0) {
          val target = if (zoom.zoomed) zoom.target else fitTarget(viewport, laidOut)
          scaleX = target.scale
          scaleY = target.scale
          translationX = target.offset.x
          translationY = target.offset.y
          transformOrigin = TransformOrigin(0f, 0f)
        }
      }
    }
  }
}

private val ZoomPadding = 16.dp

/**
 * The tappable parts currently in a preview.
 *
 * @param actionLabels what tapping each part does, for accessibility services.
 */
internal class PreviewParts(
  val actionLabels: Map<ContrastCheck, String>,
  val onClick: (ContrastCheck) -> Unit,
) {

  /** Where the layer taking taps sits, which tap positions are relative to. */
  var layer: LayoutCoordinates? = null

  private val nodes = mutableListOf<PreviewTapNode>()

  fun add(node: PreviewTapNode) {
    nodes += node
  }

  fun remove(node: PreviewTapNode) {
    nodes -= node
  }

  /**
   * Where the first part [check] covers shows, reading top to bottom, then start to end. A part
   * mostly cut off, like the name of a shelf running off the bottom of the scene, only counts when
   * no other shows.
   */
  fun boundsOf(check: ContrastCheck): Rect? {
    val layer = layer?.takeIf { it.isAttached } ?: return null
    val shown = nodes
      .filter { it.check == check }
      .mapNotNull { node ->
        val visible = node.visibleBounds(layer) ?: return@mapNotNull null
        val whole = node.bounds(layer, clip = false)
        visible to (visible.width * visible.height >= whole.width * whole.height / 2)
      }
    return shown
      .filter { (_, mostlyShows) -> mostlyShows }
      .ifEmpty { shown }
      .map { (visible, _) -> visible }
      .minWithOrNull(compareBy<Rect>({ it.top }, { it.left }))
  }

  /** Taps the smallest part showing at [position], if there is one. */
  fun tap(position: Offset) {
    val layer = layer?.takeIf { it.isAttached } ?: return
    nodes
      .mapNotNull { node -> node.visibleBounds(layer)?.takeIf { it.contains(position) }?.let { node to it } }
      .minByOrNull { (_, bounds) -> bounds.width * bounds.height }
      ?.first
      ?.tap(this)
  }
}

private val LocalPreviewParts = staticCompositionLocalOf<PreviewParts?> { null }

private data class PreviewTapElement(
  val check: ContrastCheck?,
  val onClick: (() -> Unit)?,
) : ModifierNodeElement<PreviewTapNode>() {
  override fun create() = PreviewTapNode(check, onClick)

  override fun update(node: PreviewTapNode) {
    node.check = check
    node.onClick = onClick
    node.invalidateSemantics()
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "previewTap"
    properties["check"] = check
  }
}

internal class PreviewTapNode(
  var check: ContrastCheck?,
  var onClick: (() -> Unit)?,
) : Modifier.Node(),
  CompositionLocalConsumerModifierNode,
  GlobalPositionAwareModifierNode,
  SemanticsModifierNode {

  private var parts: PreviewParts? = null
  private var coordinates: LayoutCoordinates? = null

  override fun onAttach() {
    parts = currentValueOf(LocalPreviewParts)?.also { it.add(this) }
  }

  override fun onDetach() {
    parts?.remove(this)
    parts = null
    coordinates = null
  }

  override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
    this.coordinates = coordinates
  }

  /** Where this part shows within [layer], cut to what its parents leave visible. */
  fun visibleBounds(layer: LayoutCoordinates): Rect? =
    coordinates?.takeIf { it.isAttached }?.let { bounds(layer, clip = true) }?.takeUnless { it.isEmpty }

  /** Where this part sits within [layer], cut to what its parents leave visible when [clip] is set. */
  fun bounds(layer: LayoutCoordinates, clip: Boolean): Rect {
    val coordinates = coordinates?.takeIf { it.isAttached } ?: return Rect.Zero
    return layer.localBoundingBoxOf(coordinates, clipBounds = clip)
  }

  fun tap(parts: PreviewParts) {
    check?.let(parts.onClick) ?: onClick?.invoke()
  }

  override fun SemanticsPropertyReceiver.applySemantics() {
    val check = check ?: return
    val parts = parts ?: return
    role = Role.Button
    onClick(label = parts.actionLabels[check]) {
      parts.onClick(check)
      true
    }
  }
}

/** Amber reads against both light and dark themes, whatever their accent. */
private val WarningOutlineColor = Color(0xFFF59E0B)
