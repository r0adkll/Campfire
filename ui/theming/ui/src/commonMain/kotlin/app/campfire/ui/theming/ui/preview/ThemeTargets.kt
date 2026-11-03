// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
 * With a [focus], the preview zooms in on the part it checks, so it shows at a size the user can
 * judge, and zooms back out when [focus] clears.
 */
@Composable
internal fun TappablePreviewParts(
  targets: ThemeTargets,
  actionLabel: @Composable (ContrastCheck) -> String,
  modifier: Modifier = Modifier,
  focus: ContrastCheck? = null,
  content: @Composable () -> Unit,
) {
  val onClick = targets.onClick
  if (onClick == null) {
    Box(modifier) { content() }
    return
  }

  val currentOnClick by rememberUpdatedState(onClick)
  val labels = ContrastCheck.entries.associateWith { actionLabel(it) }
  val parts = remember(labels) { PreviewParts(labels) { currentOnClick(it) } }
  val zoom = remember { PreviewZoom() }
  var size by remember { mutableStateOf(IntSize.Zero) }
  val padding = with(LocalDensity.current) { ZoomPadding.toPx() }

  LaunchedEffect(parts, focus, size) {
    // Wait for the parts to lay out at this size before reading where the focused one is.
    withFrameNanos { }
    val bounds = focus?.let(parts::boundsOf)
    zoom.animateTo(if (bounds == null) ZoomTarget.None else zoomTo(bounds, size.toSize(), padding))
  }

  CompositionLocalProvider(LocalPreviewParts provides parts) {
    Box(
      modifier
        .clipToBounds()
        .onSizeChanged { size = it },
    ) {
      // Taps and part bounds are both measured inside the zoomed layer, so neither needs undoing.
      Box(
        Modifier
          .matchParentSize()
          .graphicsLayer {
            scaleX = zoom.scale.value
            scaleY = zoom.scale.value
            translationX = zoom.x.value
            translationY = zoom.y.value
            transformOrigin = TransformOrigin(0f, 0f)
          },
      ) {
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
    }
  }
}

/** How far the preview zooms and where it moves to, so the zoomed content still fills it. */
@Immutable
internal data class ZoomTarget(val scale: Float, val offset: Offset) {
  companion object {
    val None = ZoomTarget(1f, Offset.Zero)
  }
}

/**
 * Zooms a preview of [size] in on [part], centering it as far as the preview's edges allow.
 * The whole part stays in sight, with [padding] around it, so a part as wide as the preview, like
 * the navigation bar, doesn't zoom at all: cropping it hid the selected tab. It never zooms out,
 * nor in past [MaxZoom].
 */
internal fun zoomTo(part: Rect, size: Size, padding: Float): ZoomTarget {
  if (size.isEmpty() || part.isEmpty) return ZoomTarget.None
  val width = part.width + padding * 2
  val height = part.height + padding * 2
  val scale = minOf(size.width / width, size.height / height).coerceIn(1f, MaxZoom)
  val x = (size.width / 2 - part.center.x * scale).coerceIn(size.width - size.width * scale, 0f)
  val y = (size.height / 2 - part.center.y * scale).coerceIn(size.height - size.height * scale, 0f)
  return ZoomTarget(scale, Offset(x, y))
}

private const val MaxZoom = 2.5f
private val ZoomPadding = 16.dp

private class PreviewZoom {
  val scale = Animatable(1f)
  val x = Animatable(0f)
  val y = Animatable(0f)

  suspend fun animateTo(target: ZoomTarget) = coroutineScope {
    val spec = tween<Float>(durationMillis = 350, easing = FastOutSlowInEasing)
    launch { scale.animateTo(target.scale, spec) }
    launch { x.animateTo(target.offset.x, spec) }
    launch { y.animateTo(target.offset.y, spec) }
  }
}

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

  /** Where the first part [check] covers shows, reading top to bottom, then start to end. */
  fun boundsOf(check: ContrastCheck): Rect? {
    val layer = layer?.takeIf { it.isAttached } ?: return null
    return nodes
      .filter { it.check == check }
      .mapNotNull { it.visibleBounds(layer) }
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
  fun visibleBounds(layer: LayoutCoordinates): Rect? {
    val coordinates = coordinates?.takeIf { it.isAttached } ?: return null
    return layer.localBoundingBoxOf(coordinates, clipBounds = true).takeUnless { it.isEmpty }
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
