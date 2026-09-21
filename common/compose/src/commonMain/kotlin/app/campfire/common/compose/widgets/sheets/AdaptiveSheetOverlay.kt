// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.sheets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.back.OverlayPriorityBackHandler
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * An [Overlay] that shows [content] as a bottom sheet where the region has the height for one, and
 * as a side sheet where it does not.
 *
 * The bottom case is Material's own [BottomSheetOverlay], unchanged. The side case is that sheet
 * turned ninety degrees, and deliberately built the way `ModalBottomSheet` is: sized to its content
 * on the axis it slides along and capped there, filling the other axis, and driven by an
 * [AnchoredDraggableState] with Material's own fling behaviour rather than a hand-rolled distance
 * threshold. So it follows the finger, settles where a bottom sheet would settle, and stretches for
 * content that needs the room — the equalizer's ten faders, say — rather than forcing every sheet
 * through one fixed width.
 *
 * It is drawn **in composition**, inside the `ContentWithOverlays` that launched it, which the
 * bottom sheet cannot be: `ModalBottomSheet` renders into a `ModalBottomSheetDialog` covering the
 * whole window, so a sheet opened from a player docked into half a folded screen spans the hinge
 * and covers both halves. `SheetRegionContainmentTest` pins that difference down.
 */
class AdaptiveSheetOverlay<Model : Any, Result : Any>(
  private val model: Model,
  private val onDismiss: () -> Result,
  private val skipPartiallyExpanded: Boolean = false,
  /**
   * True where [content] draws its own drag handle — the sheets whose scrolled-title bar has to
   * own it — so the sheet does not draw a second one above it.
   */
  private val contentDrawsDragHandle: Boolean = false,
  private val content: @Composable (Model, OverlayNavigator<Result>) -> Unit,
) : Overlay<Result> {

  @Composable
  override fun Content(navigator: OverlayNavigator<Result>) {
    // Read from inside the overlay, so a host that narrowed the size class for its own region --
    // the docked player does exactly that -- gets its region's verdict, not the window's.
    val presentation = LocalWindowSizeClass.current.sheetPresentation()

    CompositionLocalProvider(LocalSheetPresentation provides presentation) {
      when (presentation) {
        SheetPresentation.Bottom -> BottomSheetOverlay(
          model = model,
          onDismiss = onDismiss,
          sheetShape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
          skipPartiallyExpandedState = skipPartiallyExpanded,
          dragHandle = if (contentDrawsDragHandle) ({ }) else null,
          content = content,
        ).Content(navigator)

        SheetPresentation.Side -> SideSheet(navigator)
      }
    }
  }

  @Composable
  private fun SideSheet(navigator: OverlayNavigator<Result>) {
    val scope = rememberCoroutineScope()
    val currentNavigator by rememberUpdatedState(navigator)

    // Anchors are in raw x translation, so their sign carries the layout direction: a trailing
    // sheet hides to the right, and the same sheet in right-to-left hides to the left. The offset
    // modifier then applies them as they are, with no mirroring of its own.
    val hiddenSign = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else -1f

    val state = remember { AnchoredDraggableState(initialValue = SideSheetValue.Hidden) }
    var sheetWidthPx by remember { mutableFloatStateOf(0f) }
    var pendingResult by remember { mutableStateOf<Result?>(null) }

    val dismiss: () -> Unit = {
      if (pendingResult == null) pendingResult = onDismiss()
      scope.launch { state.animateTo(SideSheetValue.Hidden) }
    }

    // Back closes the sheet before anything else acts on it. Overlay priority so it beats the main
    // back stack whatever the composition order, and composed inside the overlay so it also covers
    // hosts with no central handler of their own -- the docked player.
    OverlayPriorityBackHandler(
      enabled = state.targetValue != SideSheetValue.Hidden,
      onBack = dismiss,
    )

    // The sheet is sized by its content, so its anchors are only known once it has been measured.
    LaunchedEffect(sheetWidthPx, hiddenSign) {
      if (sheetWidthPx <= 0f) return@LaunchedEffect
      state.updateAnchors(
        newAnchors = DraggableAnchors {
          SideSheetValue.Expanded at 0f
          SideSheetValue.Hidden at sheetWidthPx * hiddenSign
        },
        newTarget = if (pendingResult == null) SideSheetValue.Expanded else SideSheetValue.Hidden,
      )
    }

    // Settled back at Hidden -- by a drag, the scrim, back, or a row being chosen -- so the caller
    // gets its result once the sheet is actually gone rather than the moment it was asked to go.
    LaunchedEffect(state.settledValue, sheetWidthPx) {
      if (sheetWidthPx > 0f && state.settledValue == SideSheetValue.Hidden) {
        currentNavigator.finish(pendingResult ?: onDismiss())
      }
    }

    BoxWithConstraints(
      // Its own traversal group, so a screen reader reads the sheet before wandering back into
      // the content it covers.
      Modifier.fillMaxSize().semantics { isTraversalGroup = true },
    ) {
      // A bottom sheet leaves part of the screen showing however tall its content is, and this
      // has to do the same however wide the region is. The cap alone is not enough: a player
      // floating in a 700dp box would keep only a sliver of itself beside a 640dp sheet.
      val maxSheetWidth = minOf(
        SheetMaxWidth,
        maxWidth * SheetMaxWidthFraction,
        maxWidth - MinScrimStrip,
      )
      val regionWidthPx = constraints.maxWidth.toFloat()

      val scrimVisible by remember {
        derivedStateOf { state.targetValue != SideSheetValue.Hidden }
      }
      val scrimAlpha by animateFloatAsState(if (scrimVisible) 1f else 0f)

      Box(
        Modifier
          .fillMaxSize()
          .alpha(scrimAlpha)
          .background(BottomSheetDefaults.ScrimColor)
          .pointerInput(Unit) { detectTapGestures { dismiss() } },
      )

      val dragInteractions = remember { MutableInteractionSource() }

      Surface(
        // Rounded only where it meets the content it slid over; the far edge is the screen's.
        // topStart/bottomStart is the inner edge in either layout direction.
        shape = RoundedCornerShape(topStart = SheetCorner, bottomStart = SheetCorner),
        color = BottomSheetDefaults.ContainerColor,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          // Sized by its content up to a cap, the way a bottom sheet's height is.
          .widthIn(max = maxSheetWidth)
          .fillMaxHeight()
          .offset {
            // Until it has been measured there are no anchors and no offset, so park it off the
            // region's edge rather than letting it flash into view at rest.
            val offset = state.offset.takeIf { !it.isNaN() } ?: (regionWidthPx * hiddenSign)
            IntOffset(offset.roundToInt(), 0)
          }
          .onSizeChanged { sheetWidthPx = it.width.toFloat() }
          .anchoredDraggable(
            state = state,
            orientation = Orientation.Horizontal,
            enabled = state.settledValue != SideSheetValue.Hidden,
            interactionSource = dragInteractions,
            flingBehavior = AnchoredDraggableDefaults.flingBehavior(state),
          )
          // Taps inside the sheet belong to the sheet; they must not fall through to the scrim.
          .pointerInput(Unit) { detectTapGestures {} },
      ) {
        // Insets pad the content, not the surface — the way ModalBottomSheet's
        // contentWindowInsets does. Padding the surface shrank the sheet away from its region's
        // edges, which is what left it short of the bottom in a docked player.
        Row(
          Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
          // The same affordance the bottom sheet gets, turned ninety degrees, sharing the drag's
          // interaction source so it reacts while the sheet is being moved.
          Box(
            modifier = Modifier.fillMaxHeight().width(DragHandleSlotWidth),
            contentAlignment = Alignment.Center,
          ) {
            VerticalDragHandle(interactionSource = dragInteractions)
          }
          content(model) { result ->
            pendingResult = result
            scope.launch { state.animateTo(SideSheetValue.Hidden) }
          }
        }
      }
    }
  }
}

/** Where the side sheet can rest: on screen, or off the edge it came from. */
private enum class SideSheetValue {
  Hidden,
  Expanded,
}

/** Rounded corner shared with `bottomSheetShape`, so both presentations read as the same sheet. */
private val SheetCorner = 32.dp

/**
 * The widest the side sheet grows before its content has to wrap — the mirror of
 * `BottomSheetDefaults.SheetMaxWidth`, which is what caps a bottom sheet's cross axis. Content
 * that wants less makes a narrower sheet.
 */
private val SheetMaxWidth = 640.dp

/**
 * The most of its region the sheet will take. A bottom sheet leaves part of the screen showing
 * whatever its content's height, and a side sheet has to leave part of its region showing whatever
 * the content's width — including when that region is a player floating in a corner rather than
 * the whole screen.
 */
private const val SheetMaxWidthFraction = 0.72f

/** How much of the region behind the sheet stays tappable, however wide the content wants to be. */
private val MinScrimStrip = 56.dp

/** The strip down the sheet's inner edge that the drag handle sits in. */
private val DragHandleSlotWidth = 24.dp
