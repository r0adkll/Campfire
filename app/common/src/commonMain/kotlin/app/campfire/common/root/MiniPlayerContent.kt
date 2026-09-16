// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.root

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.sessions.ui.player.DedicatedPlayer
import app.campfire.sessions.ui.player.MiniPlayerAction
import app.campfire.sessions.ui.player.WideChromePlacement
import app.campfire.settings.api.CampfireSettings
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.colorScheme
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.lifecycleRetainedStateRegistry
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias MiniPlayerContent = @Composable (
  onItemClick: (LibraryItemId) -> Unit,
  onReturnToWindow: () -> Unit,
  onUnavailable: () -> Unit,
  topBarDecorator: @Composable (content: @Composable () -> Unit) -> Unit,
  modifier: Modifier,
) -> Unit

/**
 * Everything the mini-player window needs around the player: the window's own size class and
 * retained-state registry, the signed-in user's component and theme, and the Circuit locals the
 * player's sheets rely on. [onUnavailable] fires when there is no signed-in user to show a player
 * for, so the host can take the window down rather than leave it blank.
 */
@Inject
@Composable
fun MiniPlayerContent(
  @Assisted onItemClick: (LibraryItemId) -> Unit,
  @Assisted onReturnToWindow: () -> Unit,
  @Assisted onUnavailable: () -> Unit,
  @Assisted topBarDecorator: @Composable (content: @Composable () -> Unit) -> Unit,
  settings: CampfireSettings,
  userSessionManager: UserSessionManager,
  themeRepository: AppThemeRepository,
  @Assisted modifier: Modifier = Modifier,
) {
  CompositionLocalProvider(
    LocalWindowSizeClass provides currentWindowSizeClass(),
    LocalRetainedStateRegistry provides lifecycleRetainedStateRegistry(),
  ) {
    UserComponentContent(userSessionManager) { userComponent ->
      val session = userComponent.currentUserSession
      if (session !is UserSession.LoggedIn) {
        LaunchedEffect(Unit) { onUnavailable() }
        return@UserComponentContent
      }

      val appTheme by remember {
        themeRepository.observeCurrentAppTheme()
      }.collectAsState()

      CircuitCompositionLocals(userComponent.circuit) {
        CampfireTheme(
          colorScheme = { colorScheme(appTheme) },
          useDarkColors = settings.shouldUseDarkColors(),
        ) {
          key(session.requiredUserId) {
            DedicatedPlayer(
              onItemClick = { clicked -> onItemClick(clicked.libraryItem.id) },
              shape = RoundedCornerShape(MiniPlayerCornerRadius),
              navigationIcon = {
                MiniPlayerAction(isOpen = true, onClick = onReturnToWindow)
              },
              topBarDecorator = topBarDecorator,
              wideChrome = WideChromePlacement.LeadingRail,
              modifier = modifier,
            )
          }
        }
      }
    }
  }
}

/** Matches the expanded sheet's corners, which is what this window is a lifted-out copy of. */
private val MiniPlayerCornerRadius = 24.dp
