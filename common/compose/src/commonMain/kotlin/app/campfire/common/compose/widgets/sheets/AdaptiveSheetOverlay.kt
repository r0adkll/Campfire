// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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

/**
 * An [Overlay] that shows [content] in whichever of the three [SheetPresentation]s suits the region
 * hosting it, and hands a [Result] back to the caller when it closes.
 *
 * [SheetPresentation.Bottom] is Material's own [BottomSheetOverlay], unchanged. The other two are
 * drawn **in composition**, inside the `ContentWithOverlays` that launched them, rather than in a
 * dialog window of their own. That is the point of them: a `ModalBottomSheet` renders into a
 * `ModalBottomSheetDialog` that covers the whole window, so a sheet opened from a player docked
 * into half a folded screen spans the hinge and covers both halves. A side panel stays in its half.
 */
class AdaptiveSheetOverlay<Model : Any, Result : Any>(
  private val model: Model,
  private val onDismiss: () -> Result,
  /**
   * What a wide, short region gets in place of the side panel. The equalizer passes
   * [SheetPresentation.Dialog]: ten vertical band sliders want height, and a 400dp panel down the
   * edge of a short region has less of it than the region does. Tall regions are unaffected — they
   * keep the bottom sheet either way.
   */
  private val shortRegionPresentation: SheetPresentation = SheetPresentation.Side,
  private val skipPartiallyExpanded: Boolean = false,
  /**
   * True where [content] draws its own drag handle — the sheets whose scrolled-title bar has to
   * own it — so the bottom sheet does not draw a second one above it.
   */
  private val contentDrawsDragHandle: Boolean = false,
  private val content: @Composable (Model, OverlayNavigator<Result>) -> Unit,
) : Overlay<Result> {

  @Composable
  override fun Content(navigator: OverlayNavigator<Result>) {
    // Read from inside the overlay, so a host that narrowed the size class for its own region --
    // the docked player does exactly that -- gets its region's verdict, not the window's.
    val presentation = when (val chosen = LocalWindowSizeClass.current.sheetPresentation()) {
      SheetPresentation.Side -> shortRegionPresentation
      else -> chosen
    }

    CompositionLocalProvider(LocalSheetPresentation provides presentation) {
      if (presentation == SheetPresentation.Bottom) {
        BottomSheetOverlay(
          model = model,
          onDismiss = onDismiss,
          sheetShape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
          skipPartiallyExpandedState = skipPartiallyExpanded,
          dragHandle = if (contentDrawsDragHandle) ({ }) else null,
          content = content,
        ).Content(navigator)
      } else {
        InComposition(navigator, presentation)
      }
    }
  }

  /**
   * The side panel and the centred card. Both scrim the region and animate themselves in and out,
   * and both hold the caller's result back until the exit has played -- the way the bottom sheet
   * does, and for the same reason: otherwise the panel vanishes the instant a row is tapped.
   */
  @Composable
  private fun InComposition(
    navigator: OverlayNavigator<Result>,
    presentation: SheetPresentation,
  ) {
    val isSide = presentation == SheetPresentation.Side
    val sign = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1

    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    var pendingResult by remember { mutableStateOf<Result?>(null) }
    val currentNavigator by rememberUpdatedState(navigator)

    val dismiss = {
      if (pendingResult == null) pendingResult = onDismiss()
      visibleState.targetState = false
    }

    // Back closes the sheet before anything else acts on it. Overlay priority so it beats the main
    // back stack whatever the composition order, and composed inside the overlay so it also covers
    // hosts with no central handler of their own -- the docked player.
    OverlayPriorityBackHandler(enabled = visibleState.targetState, onBack = dismiss)

    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
      if (visibleState.isIdle && !visibleState.currentState) {
        currentNavigator.finish(pendingResult ?: onDismiss())
      }
    }

    BoxWithConstraints(
      // Its own traversal group, so a screen reader reads the panel before wandering back into
      // the content it covers.
      Modifier.fillMaxSize().semantics { isTraversalGroup = true },
    ) {
      // Always leave a strip of the region tappable, however narrow it gets.
      val panelWidth = minOf(SidePanelWidth, maxWidth - MinScrimStrip)

      AnimatedVisibility(visibleState, enter = fadeIn(), exit = fadeOut()) {
        Box(
          Modifier
            .fillMaxSize()
            .background(BottomSheetDefaults.ScrimColor)
            .pointerInput(Unit) { detectTapGestures { dismiss() } },
        )
      }

      AnimatedVisibility(
        visibleState = visibleState,
        // CenterEnd already resolves to the trailing edge in either layout direction; only the
        // slide offset below is in raw pixels and needs the sign.
        modifier = Modifier.align(if (isSide) Alignment.CenterEnd else Alignment.Center),
        enter = if (isSide) {
          slideInHorizontally(SlideSpring) { it * sign } + fadeIn()
        } else {
          scaleIn(initialScale = 0.9f) + fadeIn()
        },
        exit = if (isSide) {
          slideOutHorizontally(SlideSpring) { it * sign } + fadeOut()
        } else {
          scaleOut(targetScale = 0.9f) + fadeOut()
        },
      ) {
        var travelled by remember { mutableFloatStateOf(0f) }
        val dragInteractions = remember { MutableInteractionSource() }

        Surface(
          // Rounded only where it meets the content it slid over; the far edge is the screen's.
          // topStart/bottomStart is the inner edge in either layout direction.
          shape = if (isSide) {
            RoundedCornerShape(topStart = SheetCorner, bottomStart = SheetCorner)
          } else {
            RoundedCornerShape(SheetCorner)
          },
          color = BottomSheetDefaults.ContainerColor,
          modifier = Modifier
            .then(
              if (isSide) {
                Modifier
                  .width(panelWidth)
                  .fillMaxHeight()
                  // Swiping the panel back towards the edge it came from closes it, the way
                  // dragging a bottom sheet downwards does.
                  .draggable(
                    state = rememberDraggableState { delta -> travelled += delta },
                    orientation = Orientation.Horizontal,
                    interactionSource = dragInteractions,
                    onDragStopped = { velocity ->
                      val outwards = travelled * sign
                      val flung = velocity * sign
                      if (outwards > SwipeDismissDistance || flung > SwipeDismissVelocity) dismiss()
                      travelled = 0f
                    },
                  )
              } else {
                Modifier.padding(DialogMargin)
              },
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            // Taps inside the panel belong to the panel; they must not fall through to the scrim.
            .pointerInput(Unit) { detectTapGestures {} },
        ) {
          val body: @Composable () -> Unit = {
            content(model) { result ->
              pendingResult = result
              visibleState.targetState = false
            }
          }

          if (isSide) {
            // The panel is draggable anywhere, but nothing said so. A handle down its inner edge
            // is the same affordance the bottom sheet gets, turned ninety degrees — and it shares
            // the drag's interaction source, so it reacts while the panel is being moved.
            Row(Modifier.fillMaxSize()) {
              Box(
                modifier = Modifier.fillMaxHeight().width(DragHandleSlotWidth),
                contentAlignment = Alignment.Center,
              ) {
                VerticalDragHandle(interactionSource = dragInteractions)
              }
              Box(Modifier.weight(1f)) { body() }
            }
          } else {
            body()
          }
        }
      }
    }
  }
}

private val SlideSpring = spring<IntOffset>(
  dampingRatio = Spring.DampingRatioNoBouncy,
  stiffness = Spring.StiffnessMediumLow,
)

/** Rounded corner shared with `bottomSheetShape`, so every presentation reads as the same sheet. */
private val SheetCorner = 32.dp

/**
 * The side panel's width. Flat rather than a fraction of the region: across the range that occurs a
 * fraction lands on the same number anyway (0.45 of an 892dp landscape phone is 401dp), and the
 * content is list-shaped, so it wants a measure that does not drift with the device.
 */
private val SidePanelWidth = 400.dp

/** How much of the region behind the panel stays tappable, even on the narrowest of them. */
private val MinScrimStrip = 56.dp

/** The margin around the centred card, which is otherwise as large as the region allows. */
private val DialogMargin = 16.dp

/** The strip down the panel's inner edge that the drag handle sits in. */
private val DragHandleSlotWidth = 24.dp

/** Pixels of outward drag, or pixels-per-second of outward fling, that close the panel. */
private const val SwipeDismissDistance = 160f
private const val SwipeDismissVelocity = 400f
