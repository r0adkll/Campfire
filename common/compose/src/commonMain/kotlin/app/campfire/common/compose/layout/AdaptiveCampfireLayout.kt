package app.campfire.common.compose.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.Platform
import app.campfire.common.compose.currentPlatform
import app.campfire.common.compose.navigation.LocalDrawerState
import app.campfire.common.compose.navigation.LocalUserSession
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.core.extensions.fluentIf
import app.campfire.core.session.isLoggedIn
import com.moriatsushi.insetsx.navigationBars
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.OverlayHost

/**
 * Our custom adaptive layout for organizing the root level content and navigation for various
 * screen classes and orientations.
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

  content: @Composable () -> Unit,
  playbackBarContent: @Composable BoxScope.() -> Unit,
  supportingContent: @Composable () -> Unit,
  showSupportingContent: Boolean,

  modifier: Modifier = Modifier,
  hideBottomNav: Boolean = false,
  windowInsets: WindowInsets = WindowInsets.systemBars,
) {
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)
  val navigationType = remember(windowSizeClass) {
    windowSizeClass.navigationType
  }
  val isSupportingPaneEnabled = remember(windowSizeClass) {
    windowSizeClass.isSupportingPaneEnabled
  }
  val supportingContentState =
    if (
      (
        showSupportingContent ||
          windowSizeClass.widthSizeClass == WindowWidthSizeClass.ExtraLarge
        ) &&
      isSupportingPaneEnabled
    ) {
      SupportingContentState.Open
    } else {
      SupportingContentState.Closed
    }

  val isLoggedIn by rememberUpdatedState(LocalUserSession.current.isLoggedIn)

  ContentWithOverlays(
    overlayHost = overlayHost,
  ) {
    // This wraps a ModalNavigationDrawer IF the navigationType is Rail or BottomNav
    // otherwise, this just pass the content() block through
    Column(modifier) {
      DrawerWithContent(
        navigationType = navigationType,
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = isLoggedIn && drawerEnabled,
        modifier = Modifier.weight(1f),
      ) {
        Scaffold(
          bottomBar = {
            if (navigationType == NavigationType.BottomNavigation) {
              AnimatedVisibility(
                visible = !hideBottomNav,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
              ) {
                bottomBarNavigation()
              }
            } else {
              Spacer(
                Modifier
                  .windowInsetsBottomHeight(WindowInsets.navigationBars)
                  .fillMaxWidth(),
              )
            }
          },
          contentWindowInsets = windowInsets,
        ) { paddingValues ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .fluentIf(
                navigationType == NavigationType.BottomNavigation && !hideBottomNav,
              ) {
                padding(paddingValues)
              },
          ) {
            if (navigationType == NavigationType.Rail && isLoggedIn) {
              railNavigation()
            }

            val targetWidth = windowSizeClass.SupportingContentWidth
            val supportingContentWidth by animateDpAsState(
              if (supportingContentState == SupportingContentState.Open && isSupportingPaneEnabled) {
                targetWidth
              } else {
                0.dp
              },
            )

            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            ) {
              if (isSupportingPaneEnabled && isLoggedIn) {
                var query by remember { mutableStateOf("") }
                var isActive by remember { mutableStateOf(false) }
                DockedSearchBar(
                  query = query,
                  onQueryChange = {
                    query = it
                  },
                  onSearch = {
                  },
                  active = isActive,
                  onActiveChange = {
                    isActive = it
                  },
                  leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                  },
                  trailingIcon = {
                    androidx.compose.animation.AnimatedVisibility(
                      visible = query.isNotBlank() || isActive,
                      enter = fadeIn(),
                      exit = fadeOut(),
                    ) {
                      IconButton(
                        onClick = {
                          query = ""
                          isActive = false
                        },
                      ) {
                        Icon(Icons.Rounded.Clear, contentDescription = null)
                      }
                    }
                  },
                  placeholder = {
                    Text("Search for your next story…")
                  },
                  modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(end = supportingContentWidth)
                    .fluentIf(currentPlatform == Platform.DESKTOP) {
                      padding(top = 16.dp)
                    },
                ) {
                  EmptyState(
                    "Start typing to start lookin'",
                  )
                }
              }

              Column(
                modifier = Modifier.padding(end = supportingContentWidth),
              ) {
                if (isSupportingPaneEnabled && isLoggedIn) {
                  Spacer(
                    Modifier
                      .statusBarsPadding()
                      .fluentIf(currentPlatform == Platform.DESKTOP) {
                        padding(top = 16.dp)
                      }
                      .height(SearchBarDefaults.InputFieldHeight),
                  )
                }
                CompositionLocalProvider(
                  LocalContentLayout provides ContentLayout.Root,
                  LocalSupportingContentState provides supportingContentState,
                ) {
                  Box {
                    content()

                    if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.ExtraLarge) {
                      playbackBarContent()
                    }
                  }
                }
              }

              if (isSupportingPaneEnabled && isLoggedIn) {
                val supportingContentShape = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.ExtraLarge) {
                  RoundedCornerShape(
                    topStart = SupportingContentCornerRadius,
                  )
                } else {
                  RoundedCornerShape(
                    topStart = SupportingContentCornerRadius,
                    bottomStart = SupportingContentCornerRadius,
                  )
                }
                Surface(
                  modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(windowSizeClass.SupportingContentWidth)
                    .offset {
                      IntOffset(
                        (windowSizeClass.SupportingContentWidth - supportingContentWidth).roundToPx(),
                        0,
                      )
                    },
                  shadowElevation = SupportingContentElevation,
                  tonalElevation = SupportingContentElevation,
                  shape = supportingContentShape,
                ) {
                  CompositionLocalProvider(
                    LocalContentLayout provides ContentLayout.Supporting,
                    LocalSupportingContentState provides supportingContentState,
                  ) {
                    supportingContent()
                  }
                }
              }
            }
          }
        }
      }

      if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.ExtraLarge) {
        Box(modifier = Modifier.fillMaxWidth()) {
          playbackBarContent()
        }
      }
    }
  }
}

@Composable
private fun DrawerWithContent(
  navigationType: NavigationType,
  modifier: Modifier = Modifier,
  gesturesEnabled: Boolean = true,
  drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
  drawerContent: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  if (navigationType == NavigationType.BottomNavigation || navigationType == NavigationType.Rail) {
    CompositionLocalProvider(
      LocalDrawerState provides drawerState,
    ) {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = gesturesEnabled,
        modifier = modifier,
      ) {
        content()
      }
    }
  } else if (navigationType == NavigationType.Drawer) {
    PermanentNavigationDrawer(
      drawerContent = drawerContent,
      modifier = modifier,
    ) {
      content()
    }
  } else {
    content()
  }
}

val SupportingContentElevation = 6.dp
val SupportingContentCornerRadius = 32.dp

val SupportingContentWidthExpanded = 360.dp
val SupportingContentWidthLarge = 400.dp
val SupportingContentWidthExtraLarge = 500.dp

val WindowSizeClass.SupportingContentWidth: Dp
  get() = when (widthSizeClass) {
    WindowWidthSizeClass.ExtraLarge -> SupportingContentWidthExtraLarge
    WindowWidthSizeClass.Large -> SupportingContentWidthLarge
    else -> SupportingContentWidthExpanded
  }
