// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.computeWindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Headphones
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.theme.colorScheme
import app.campfire.core.model.Session
import app.campfire.sessions.ui.playback.CampfirePlaybackBarComponent
import app.campfire.sessions.ui.playback.DefaultNonThemedContentColor
import app.campfire.sessions.ui.playback.DefaultNonThemedSheetColor
import app.campfire.sessions.ui.playback.DefaultSheetColor
import app.campfire.sessions.ui.playback.DefaultSheetContentColor
import app.campfire.sessions.ui.playback.PlaybackUiState
import app.campfire.sessions.ui.playback.QueueUiEvent
import app.campfire.sessions.ui.playback.expanded.ExpandedPlaybackContent
import app.campfire.sessions.ui.playback.expanded.composables.ExpandedPlaybackRail
import app.campfire.sessions.ui.playback.expanded.composables.ExpandedPlaybackTopBar
import app.campfire.sessions.ui.playback.expanded.composables.PlaybackQueueSwitcher
import app.campfire.sessions.ui.playback.rememberCampfirePlaybackBarComponent
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.bottom_bar_nothing_playing
import campfire.features.sessions.ui.generated.resources.bottom_bar_nothing_playing_hint
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.retained.rememberRetained
import org.jetbrains.compose.resources.stringResource

/**
 * The full player as a standalone surface, for hosts that give it a region of its own rather than
 * a sheet over the content: the desktop mini-player window and the lower half of a half-open
 * foldable. It is the expanded player's body with none of its sheet chrome — no drag-to-dismiss,
 * no shared-element hand-off from the collapsed bar — and it picks one of the three
 * [DedicatedPlayerLayout]s from its own measured bounds, not the window's.
 *
 * [onItemClick] is invoked when the cover is tapped; hosts route it to whatever shows the item's
 * detail screen. [navigationIcon] fills the chrome's leading slot (a "return to window" button,
 * say), [topBarDecorator] wraps the chrome so a borderless window can make it the drag handle,
 * and [wideChrome] chooses where that chrome sits once the region is wide and short.
 */
@Composable
fun DedicatedPlayer(
  onItemClick: (Session) -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = RectangleShape,
  navigationIcon: @Composable () -> Unit = {},
  topBarDecorator: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
  wideChrome: WideChromePlacement = WideChromePlacement.TopBar,
  component: State<CampfirePlaybackBarComponent> = rememberCampfirePlaybackBarComponent(),
) {
  val comp by component
  key(comp) {
    val presenter = rememberRetained {
      comp.playbackPresenterFactory()
    }

    // Always the expanded form: this surface is the full player, and the sync banner it carries
    // wants the server progress re-fetched on every (re)appearance the way the sheet does.
    val uiState = presenter.present(expanded = true)

    DedicatedPlayerContent(
      uiState = uiState,
      onItemClick = onItemClick,
      shape = shape,
      navigationIcon = navigationIcon,
      topBarDecorator = topBarDecorator,
      wideChrome = wideChrome,
      modifier = modifier,
    )
  }
}

/** The player driven by a ready-made state, so it can be previewed and rendered in tests. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun DedicatedPlayerContent(
  uiState: PlaybackUiState,
  onItemClick: (Session) -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = RectangleShape,
  navigationIcon: @Composable () -> Unit = {},
  topBarDecorator: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
  wideChrome: WideChromePlacement = WideChromePlacement.TopBar,
) {
  MaterialExpressiveTheme(
    colorScheme = uiState.themeState.theme?.colorScheme,
  ) {
    val containerColor = if (uiState.themeState.dynamicThemingEnabled) {
      DefaultSheetColor
    } else {
      DefaultNonThemedSheetColor
    }
    val contentColor = if (uiState.themeState.dynamicThemingEnabled) {
      DefaultSheetContentColor
    } else {
      DefaultNonThemedContentColor
    }

    BoxWithConstraints(modifier) {
      // Everything inside the expanded body reads the window size class for its inset and
      // corner decisions. Feed it this region's size instead, so a wide-and-short foldable half
      // is treated like the landscape phone it resembles rather than the tall tablet it sits in.
      val regionSizeClass = remember(maxWidth, maxHeight) {
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(maxWidth.value, maxHeight.value)
      }
      val layout = remember(maxWidth, maxHeight) {
        dedicatedPlayerLayout(maxWidth, maxHeight)
      }
      val session = uiState.session
      val isCompact = layout == DedicatedPlayerLayout.Compact && session != null
      val useRail = layout == DedicatedPlayerLayout.Wide && wideChrome == WideChromePlacement.LeadingRail

      CompositionLocalProvider(LocalWindowSizeClass provides regionSizeClass) {
        val overlayHost = rememberOverlayHost()
        ContentWithOverlays(
          overlayHost = overlayHost,
          modifier = Modifier.fillMaxSize(),
        ) {
          Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            modifier = Modifier.fillMaxSize(),
          ) {
            SharedTransitionLayout {
              // The body's cover carries a shared-element modifier for the sheet's hand-off. There
              // is nothing to hand off from here, so it gets a scope that is simply always visible.
              AnimatedVisibility(
                visibleState = remember { MutableTransitionState(true) },
                enter = EnterTransition.None,
                exit = ExitTransition.None,
              ) {
                var showQueue by remember { mutableStateOf(false) }
                val queueState = uiState.queueState
                val hasQueue = queueState.queue.isNotEmpty()

                Box(Modifier.fillMaxSize()) {
                  // Compact windows put the art behind everything, chrome included, so the bar
                  // draws with no background of its own over it.
                  if (isCompact) {
                    CoverBackdrop(
                      imageUrl = uiState.playerState.metadata.artworkUri
                        ?: session?.libraryItem?.media?.coverImageUrl,
                      tint = containerColor,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }

                  // Whatever the host has not consumed — a side camera cutout, the navigation
                  // bar beneath a docked player — stays clear of the content.
                  val insetsModifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                      WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                    )

                  // A rail runs down the leading edge and a bar across the top; either way the
                  // host's decorator wraps it, so a borderless window keeps its drag handle.
                  val chrome: @Composable () -> Unit = {
                    if (useRail) {
                      ExpandedPlaybackRail(
                        hasQueue = hasQueue,
                        showQueue = showQueue,
                        onShowQueueChange = { showQueue = it },
                        onClearQueue = { queueState.eventSink(QueueUiEvent.ClearQueue) },
                        navigationIcon = navigationIcon,
                        contentColor = contentColor,
                      )
                    } else {
                      ExpandedPlaybackTopBar(
                        hasQueue = hasQueue,
                        showQueue = showQueue,
                        onShowQueueChange = { showQueue = it },
                        onClearQueue = { queueState.eventSink(QueueUiEvent.ClearQueue) },
                        navigationIcon = navigationIcon,
                        containerColor = if (isCompact) Color.Transparent else containerColor,
                        contentColor = contentColor,
                        // The host consumes whatever system bars sit above this region.
                        windowInsets = WindowInsets(0.dp),
                        queueButtonSize = if (layout == DedicatedPlayerLayout.Tall) {
                          ButtonDefaults.MinHeight
                        } else {
                          ButtonDefaults.ExtraSmallContainerHeight
                        },
                      )
                    }
                  }

                  val body: @Composable (Modifier) -> Unit = { bodyModifier ->
                    if (session == null) {
                      NothingPlaying(modifier = bodyModifier)
                    } else {
                      PlaybackQueueSwitcher(
                        showQueue = showQueue && hasQueue,
                        queueState = queueState,
                        onQueueItemChosen = { showQueue = false },
                        modifier = bodyModifier,
                      ) {
                        when (layout) {
                          DedicatedPlayerLayout.Wide -> WideDedicatedPlaybackContent(
                            overlayHost = overlayHost,
                            session = session,
                            playerState = uiState.playerState,
                            syncState = uiState.syncUiState,
                            itemValidation = uiState.validation,
                            playbackHistoryEnabled = uiState.playbackHistoryEnabled,
                            volumeState = uiState.volume,
                            outputDeviceState = uiState.outputDevices,
                            onItemClick = onItemClick,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            modifier = Modifier.fillMaxSize(),
                          )

                          DedicatedPlayerLayout.Compact -> CompactDedicatedPlaybackContent(
                            session = session,
                            playerState = uiState.playerState,
                            onItemClick = onItemClick,
                            modifier = Modifier.fillMaxSize(),
                          )

                          DedicatedPlayerLayout.Tall -> ExpandedPlaybackContent(
                            overlayHost = overlayHost,
                            session = session,
                            playerState = uiState.playerState,
                            syncState = uiState.syncUiState,
                            itemValidation = uiState.validation,
                            playbackHistoryEnabled = uiState.playbackHistoryEnabled,
                            volumeState = uiState.volume,
                            outputDeviceState = uiState.outputDevices,
                            onItemClick = onItemClick,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            modifier = Modifier.fillMaxSize(),
                          )
                        }
                      }
                    }
                  }

                  if (useRail) {
                    Row(insetsModifier) {
                      topBarDecorator(chrome)
                      Column(Modifier.weight(1f)) {
                        body(Modifier.weight(1f))
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                      }
                    }
                  } else {
                    Column(insetsModifier) {
                      topBarDecorator(chrome)
                      body(Modifier.weight(1f))
                      Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                    }
                  }
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
 * Where the player's chrome goes when the region is wide and short.
 *
 * A free-floating window asks for [LeadingRail]: it has width to spare and no height, and the
 * bar's own 64dp strip across the top left the body looking cramped and off-centre. A player
 * docked into a screen keeps [TopBar], where the trailing actions stay in the top-right corner
 * the rest of the app puts them in — a rail would strand the cast button in the bottom-left.
 */
enum class WideChromePlacement {
  TopBar,
  LeadingRail,
}

/** The three arrangements the dedicated player takes, chosen from the region it is given. */
internal enum class DedicatedPlayerLayout {
  /** The stacked phone-sheet arrangement: cover on top, everything beneath. */
  Tall,

  /** Cover, transport and tools side by side, for any wide and short region. */
  Wide,

  /** Just the transport over the cover, for a small and roughly square window. */
  Compact,
}

/**
 * Which arrangement a region of the given size gets. Anything tall enough stacks. Short regions
 * go three-column when there is the width for it, and otherwise drop to the compact transport —
 * a stack would crush the cover to nothing, and three columns would crush the controls.
 */
internal fun dedicatedPlayerLayout(width: Dp, height: Dp): DedicatedPlayerLayout {
  return when {
    height >= HEIGHT_DP_MEDIUM_LOWER_BOUND.dp -> DedicatedPlayerLayout.Tall
    width >= WIDTH_DP_MEDIUM_LOWER_BOUND.dp -> DedicatedPlayerLayout.Wide
    else -> DedicatedPlayerLayout.Compact
  }
}

/** Fills the player region between sessions, so a docked host's layout does not jump. */
@Composable
private fun NothingPlaying(
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
      CampfireIcons.Rounded.Headphones,
      contentDescription = null,
      modifier = Modifier.size(48.dp).alpha(0.6f),
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = stringResource(Res.string.bottom_bar_nothing_playing),
      style = MaterialTheme.typography.titleLarge,
      fontFamily = PaytoneOneFontFamily,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(4.dp))
    Text(
      text = stringResource(Res.string.bottom_bar_nothing_playing_hint),
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.alpha(0.7f),
    )
  }
}
