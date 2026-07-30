// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin.campfire

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.UserSessionManager
import app.campfire.android.plugin.common.SectionButton
import app.campfire.android.plugin.common.SectionHeader
import app.campfire.android.plugin.common.SegmentedSection
import app.campfire.core.session.UserSession
import app.campfire.core.session.user
import com.livewire.ui.actions.clickAction
import com.livewire.ui.layout.Alignment
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.Row
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.padding
import com.livewire.ui.modifier.verticalScroll
import com.livewire.ui.theme.LivewireTheme
import com.livewire.ui.widget.Surface
import com.livewire.ui.widget.Text
import kotlinx.coroutines.launch

/**
 * Session, account, and token debugging: the live UserSession state, current
 * user/server details, all signed-in servers with switch/logout actions, a masked
 * token inspector, and a danger zone for forcing auth failure paths.
 */
@Composable
internal fun AccountTab(
  userSessionManager: UserSessionManager,
  accountManager: AccountManager,
  serverRepository: ServerRepository,
) {
  val session by userSessionManager.observe().collectAsState()
  val servers by serverRepository.observeAllServers().collectAsState(initial = emptyList())
  val scope = rememberCoroutineScope()

  // Bumped after token mutations to re-fetch
  var tokenRefresh by remember { mutableIntStateOf(0) }
  var token by remember { mutableStateOf<AbsToken?>(null) }
  var tokenRevealed by remember { mutableStateOf(false) }

  val currentUser = session.user
  LaunchedEffect(currentUser?.id, tokenRefresh) {
    token = currentUser?.let { accountManager.getToken(it.id) }
  }

  Column(
    LivewireModifier
      .fillMaxSize()
      .verticalScroll()
      .padding(horizontal = 16.dp),
  ) {
    SectionHeader("Session")
    SegmentedSection(
      title = "",
      rows = listOf(
        "state" to session.displayName(),
        "key" to session.key.toString(),
      ),
    )

    if (currentUser != null) {
      SegmentedSection(
        title = "User",
        rows = listOf(
          "id" to currentUser.id,
          "name" to currentUser.name,
          "type" to currentUser.type.toString(),
          "isActive" to currentUser.isActive.toString(),
          "isLocked" to currentUser.isLocked.toString(),
          "selectedLibraryId" to currentUser.selectedLibraryId,
          "serverUrl" to currentUser.serverUrl,
        ),
      )

      SectionHeader("Access token") {
        SectionButton(
          action = clickAction { tokenRevealed = !tokenRevealed },
        ) { Text(if (tokenRevealed) "Hide" else "Reveal") }
      }
      SegmentedSection(
        title = "",
        rows = listOf(
          "accessToken" to (token?.accessToken?.displayToken(tokenRevealed) ?: "none"),
          "refreshToken" to (token?.refreshToken?.displayToken(tokenRevealed) ?: "none"),
        ),
      )
    }

    SectionHeader("Servers (${servers.size})")
    if (servers.isEmpty()) {
      Text("No signed-in servers.", color = Color.Gray)
    } else {
      servers.forEach { server ->
        val isCurrent = server.user.id == currentUser?.id && server.url == currentUser.serverUrl
        Surface(
          modifier = LivewireModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
          tonalElevation = 1.dp,
        ) {
          Row(
            LivewireModifier
              .fillMaxWidth()
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(LivewireModifier.weight(1f)) {
              Text(
                text = server.name + if (isCurrent) "  (current)" else "",
                style = LivewireTheme.typography.titleSmall,
                color = if (isCurrent) Color.Green else Color.Unspecified,
              )
              Text(
                text = "${server.user.name} @ ${server.url}",
                style = LivewireTheme.typography.bodySmall,
                color = Color.Gray,
              )
            }
            if (!isCurrent) {
              SectionButton(
                action = clickAction(key = "switch_${server.url}_${server.user.id}") {
                  scope.launch { accountManager.switchAccount(server.user) }
                },
                modifier = LivewireModifier.padding(2.dp),
              ) { Text("Switch") }
            }
            SectionButton(
              action = clickAction(key = "logout_${server.url}_${server.user.id}") {
                scope.launch { accountManager.logout(server) }
              },
              modifier = LivewireModifier.padding(2.dp),
            ) { Text("Logout") }
          }
        }
      }
    }

    if (currentUser != null) {
      SectionHeader("Danger zone")
      Text(
        text = "Actions that deliberately break the current session to exercise failure paths.",
        style = LivewireTheme.typography.bodySmall,
        color = Color.Gray,
      )
      Row(LivewireModifier.padding(vertical = 8.dp)) {
        SectionButton(
          action = clickAction {
            scope.launch {
              accountManager.updateToken(
                userId = currentUser.id,
                newToken = AbsToken(
                  accessToken = "corrupted-by-livewire",
                  refreshToken = token?.refreshToken,
                ),
              )
              tokenRefresh++
            }
          },
          modifier = LivewireModifier.padding(2.dp),
        ) { Text("Corrupt access token", color = Color.Red) }
        SectionButton(
          action = clickAction {
            scope.launch { accountManager.invalidateAccount(currentUser) }
          },
          modifier = LivewireModifier.padding(2.dp),
        ) { Text("Invalidate account", color = Color.Red) }
      }
    }
  }
}

private fun UserSession.displayName(): String = when (this) {
  is UserSession.LoggedIn -> "LoggedIn"
  is UserSession.NeedsAuthentication -> "NeedsAuthentication(${server.name})"
  UserSession.LoggedOut -> "LoggedOut"
  UserSession.Loading -> "Loading"
}

private fun String.displayToken(revealed: Boolean): String {
  if (revealed) return this
  return if (length > 6) "•••${takeLast(6)}" else "••••••"
}
