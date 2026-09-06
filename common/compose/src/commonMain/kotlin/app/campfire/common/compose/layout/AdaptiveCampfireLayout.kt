// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.layout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.navigation.LocalDrawerState
import app.campfire.common.compose.navigation.LocalUserSession
import app.campfire.core.Platform
import app.campfire.core.currentPlatform
import app.campfire.core.session.isLoggedIn
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.OverlayHost

/**
 * Our custom adaptive layout for organizing the root level content and navigation for various
 * screen classes and orientations.
 *
 * Navigation follows [WindowSizeClass.navigationType]: a bottom bar, a compact rail, a
 * collapsible wide rail (desktop only), or a permanent drawer. The playback bar is either floated
 * over the content or, when [WindowSizeClass.usesBottomPlaybackBar], docked full-width under
 * everything else.
 *
 * On desktop the supporting pane can be resized by dragging the handle on its leading edge.
 * [supportingContentWidth] is the user's last chosen width (`null` for the size-class default)
 * and [onSupportingContentWidthChange] is called with the new width once a drag ends; both are
 * ignored on other platforms, which keep the fixed [SupportingContentWidth].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveCampfireLayout(
  overlayHost: OverlayHost,
  drawerState: DrawerState,
  drawerEnabled: Boolean,

  drawerContent: @Composable () -> Unit,
  bottomBarNavigation: @Composable () -> Unit,
  railNavigation: @Composable () -> Unit,
  wideRailNavigation: @Composable () -> Unit,

  content: @Composable () -> Unit,
  playbackBarContent: @Composable BoxScope.() -> Unit,
  supportingContent: @Composable () -> Unit,
  showSupportingContent: Boolean,
  supportingContentWidth: Dp?,
  onSupportingContentWidthChange: (Dp) -> Unit,

  modifier: Modifier = Modifier,
) {
  val isLoggedIn by rememberUpdatedState(LocalUserSession.current.isLoggedIn)
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)
  val navigationType = remember(windowSizeClass) {
    windowSizeClass.navigationType
  }
  val isSupportingPaneEnabled = remember(windowSizeClass) {
    windowSizeClass.isSupportingPaneEnabled
  }
  val usesBottomPlaybackBar = remember(windowSizeClass) {
    windowSizeClass.usesBottomPlaybackBar
  }
  val supportingContentState =
    if (
      (showSupportingContent || windowSizeClass.isWidthAtLeastExtraLarge) &&
      isSupportingPaneEnabled &&
      isLoggedIn
    ) {
      SupportingContentState.Open
    } else {
      SupportingContentState.Closed
    }

  ContentWithOverlays(
    overlayHost = overlayHost,
  ) {
    val bottomPlaybackBar: @Composable ColumnScope.() -> Unit = {
      if (usesBottomPlaybackBar && isLoggedIn) {
        Box(modifier = Modifier.fillMaxWidth()) {
          playbackBarContent()
        }
      }
    }

    // Bottom bar and compact rail get a modal drawer, the permanent Drawer sits beside the content,
    // and the wide rail has no drawer at all (its header carries the account switcher). The docked
    // playback bar sits under the permanent drawer but inside the modal one, so the scrim covers it.
    DrawerWithContent(
      navigationType = navigationType,
      drawerState = drawerState,
      drawerContent = drawerContent,
      gesturesEnabled = isLoggedIn && drawerEnabled,
      bottomContent = bottomPlaybackBar,
      modifier = modifier,
    ) {
      // Screens handle their own system-bar and window-chrome insets, so the root scaffold
      // reserves nothing here. This keeps content edge-to-edge (the supporting pane starts at
      // the very top) and avoids exposing a blank strip above app bars when content scrolls.
      Scaffold(
        contentWindowInsets = WindowInsets(0),
      ) { paddingValues ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        ) {
          if (isLoggedIn) {
            when (navigationType) {
              NavigationType.Rail -> railNavigation()
              NavigationType.WideRail -> wideRailNavigation()
              NavigationType.BottomNavigation, NavigationType.Drawer -> Unit
            }
          }

          BoxWithConstraints(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
          ) {
            // Desktop lets the user drag the pane between a fixed minimum and the smaller of a
            // fixed maximum or a fraction of the content area; everything else keeps the
            // size-class default. Drags edit a local override that is dropped once the
            // persisted width catches up.
            val isSupportingPaneResizable = currentPlatform == Platform.DESKTOP
            val maxPaneWidth = maxOf(
              SupportingContentMinWidth,
              minOf(SupportingContentMaxWidth, maxWidth * SupportingContentMaxWidthFraction),
            )
            val basePaneWidth = if (isSupportingPaneResizable) {
              (supportingContentWidth ?: windowSizeClass.SupportingContentWidth)
                .coerceIn(SupportingContentMinWidth, maxPaneWidth)
            } else {
              windowSizeClass.SupportingContentWidth
            }
            var draggedPaneWidth by remember(supportingContentWidth) { mutableStateOf<Dp?>(null) }
            val paneWidth = draggedPaneWidth ?: basePaneWidth

            val density = LocalDensity.current
            val resizeState = rememberDraggableState { deltaPx ->
              // The pane hangs off the trailing edge, so dragging towards the start widens it.
              val delta = with(density) { deltaPx.toDp() }
              draggedPaneWidth = (paneWidth - delta).coerceIn(SupportingContentMinWidth, maxPaneWidth)
            }

            val openFraction by animateFloatAsState(
              if (supportingContentState == SupportingContentState.Open && isSupportingPaneEnabled) {
                1f
              } else {
                0f
              },
            )
            val visiblePaneWidth = paneWidth * openFraction

            Column(
              modifier = Modifier.padding(end = visiblePaneWidth),
            ) {
              CompositionLocalProvider(
                LocalContentLayout provides ContentLayout.Root,
                LocalSupportingContentState provides supportingContentState,
              ) {
                Box {
                  content()

                  if (!usesBottomPlaybackBar) {
                    playbackBarContent()
                  }
                }
              }
            }

            if (navigationType == NavigationType.BottomNavigation) {
              Box(
                modifier = Modifier.align(Alignment.BottomCenter),
              ) {
                bottomBarNavigation()
              }
            }

            if (isSupportingPaneEnabled && isLoggedIn) {
              val supportingContentShape = if (usesBottomPlaybackBar) {
                RoundedCornerShape(
                  topStart = SupportingContentCornerRadius,
                )
              } else {
                RoundedCornerShape(
                  topStart = SupportingContentCornerRadius,
                  bottomStart = SupportingContentCornerRadius,
                )
              }
              Box(
                modifier = Modifier
                  .align(Alignment.CenterEnd)
                  .width(paneWidth)
                  .fillMaxHeight()
                  .offset {
                    IntOffset((paneWidth - visiblePaneWidth).roundToPx(), 0)
                  },
              ) {
                Surface(
                  modifier = Modifier.fillMaxSize(),
                  shadowElevation = SupportingContentElevation,
                  tonalElevation = 1.dp,
                  shape = supportingContentShape,
                ) {
                  CompositionLocalProvider(
                    LocalContentLayout provides ContentLayout.Supporting,
                    LocalSupportingContentState provides supportingContentState,
                  ) {
                    supportingContent()
                  }
                }

                if (isSupportingPaneResizable) {
                  SupportingPaneResizeHandle(
                    state = resizeState,
                    onDragStopped = {
                      draggedPaneWidth?.let(onSupportingContentWidthChange)
                    },
                    modifier = Modifier
                      .align(Alignment.CenterStart)
                      .fillMaxHeight(),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Wraps [content] in the drawer that matches [navigationType]: a permanent drawer beside the
 * content (with [bottomContent] docked full-width beneath both), no drawer for the wide rail, or
 * a modal drawer over a column of the content and [bottomContent].
 */
@Composable
private fun DrawerWithContent(
  navigationType: NavigationType,
  bottomContent: @Composable ColumnScope.() -> Unit,
  modifier: Modifier = Modifier,
  gesturesEnabled: Boolean = true,
  drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
  drawerContent: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  if (navigationType == NavigationType.Drawer) {
    Column(modifier) {
      if (gesturesEnabled) {
        PermanentNavigationDrawer(
          drawerContent = drawerContent,
          modifier = Modifier.weight(1f),
        ) {
          content()
        }
      } else {
        Box(Modifier.weight(1f)) {
          content()
        }
      }

      bottomContent()
    }
  } else if (navigationType == NavigationType.WideRail) {
    Column(modifier) {
      Box(Modifier.weight(1f)) {
        content()
      }

      bottomContent()
    }
  } else {
    CompositionLocalProvider(
      LocalDrawerState provides drawerState,
    ) {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = gesturesEnabled,
        modifier = modifier,
      ) {
        Column {
          Box(Modifier.weight(1f)) {
            content()
          }

          bottomContent()
        }
      }
    }
  }
}

/**
 * The grab area straddling the supporting pane's leading edge. A Material [VerticalDragHandle]
 * sits in its centre so the affordance is visible; the whole strip is draggable and shows the
 * horizontal resize cursor.
 */
@Composable
private fun SupportingPaneResizeHandle(
  state: DraggableState,
  onDragStopped: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier = modifier
      .width(SupportingPaneResizeHitWidth)
      .offset(x = -SupportingPaneResizeHitWidth / 2)
      .hoverable(interactionSource)
      .pointerHoverIcon(horizontalResizePointerIcon)
      .draggable(
        state = state,
        orientation = Orientation.Horizontal,
        interactionSource = interactionSource,
        onDragStopped = { onDragStopped() },
      ),
    contentAlignment = Alignment.Center,
  ) {
    VerticalDragHandle(interactionSource = interactionSource)
  }
}

val SupportingContentElevation = 6.dp
val SupportingContentCornerRadius = 32.dp

/** Narrowest the desktop supporting pane can be dragged to. */
val SupportingContentMinWidth = 320.dp

/** Widest the desktop supporting pane can be dragged to, before the window-fraction cap. */
val SupportingContentMaxWidth = 640.dp

/** The desktop supporting pane never takes more than this share of the content area. */
const val SupportingContentMaxWidthFraction = 0.5f

private val SupportingPaneResizeHitWidth = 24.dp

val SupportingContentWidthExpanded = 360.dp
val SupportingContentWidthLarge = 400.dp
val SupportingContentWidthExtraLarge = 500.dp

val WindowSizeClass.SupportingContentWidth: Dp
  get() = when {
    isWidthAtLeastExtraLarge -> SupportingContentWidthExtraLarge
    isWidthAtLeastLarge -> SupportingContentWidthLarge
    else -> SupportingContentWidthExpanded
  }
