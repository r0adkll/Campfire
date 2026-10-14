// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.connect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.account.api.AccountManager
import app.campfire.account.api.ServerRepository
import app.campfire.auth.api.ConnectLinkInbox
import app.campfire.auth.api.screen.ConnectScreen
import app.campfire.auth.ui.login.withCloudflareServiceToken
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.Server
import app.campfire.core.model.isSameServerUrl
import app.campfire.core.navigation.ConnectLink
import app.campfire.core.navigation.ConnectLinkResult
import app.campfire.core.session.UserSession
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

/**
 * Confirms a setup link with the user. Nothing in the link is saved until they agree:
 *
 * - With no account on the link's server yet, it moves on to [LoginScreen.Connect], which
 *   prefills the server and the service token and, when the link asks for it, starts OpenID.
 * - With an account already on that server (Campfire keeps one per server), that account's
 *   connection headers get the new token, as editing them in Settings → Connection would, and the
 *   user stays signed in. Campfire doesn't switch to that account.
 */
@CircuitInject(ConnectScreen::class, UserScope::class)
@Inject
class ConnectPresenter(
  private val screen: ConnectScreen,
  private val navigator: Navigator,
  private val connectLinkInbox: ConnectLinkInbox,
  private val serverRepository: ServerRepository,
  private val accountManager: AccountManager,
  private val userSession: UserSession,
) : Presenter<ConnectUiState> {

  @Composable
  override fun present(): ConnectUiState {
    val coroutineScope = rememberCoroutineScope()
    val result = remember { connectLinkInbox[screen.linkId] }
    val link = (result as? ConnectLinkResult.Valid)?.link

    // The link doesn't outlive the process; with nothing left to confirm, leave
    LaunchedEffect(result) {
      if (result == null) navigator.pop()
    }

    var existingServer by remember { mutableStateOf<ExistingServer?>(null) }
    LaunchedEffect(link) {
      if (link == null) return@LaunchedEffect
      existingServer = ExistingServer(
        serverRepository.getAllServers().firstOrNull { isSameServerUrl(it.url, link.serverUrl) },
      )
    }

    var isSaving by remember { mutableStateOf(false) }
    var updatedServer by remember { mutableStateOf<Server?>(null) }

    val updated = updatedServer
    val content = when {
      result is ConnectLinkResult.UnsupportedVersion -> ConnectContent.UnsupportedVersion
      link == null -> ConnectContent.Invalid
      updated != null -> ConnectContent.Updated(link.serverHost, updated.user.name)
      else -> existingServer?.let { existing ->
        ConnectContent.Confirm(
          serverHost = link.serverHost,
          serverName = link.serverName,
          existingUserName = existing.server?.user?.name,
          signInWithOpenId = link.signInWithOpenId,
          isSaving = isSaving,
        )
      } ?: ConnectContent.Loading
    }

    return ConnectUiState(content) { event ->
      when (event) {
        ConnectUiEvent.Dismiss -> {
          connectLinkInbox.discard(screen.linkId)
          navigator.pop()
        }

        ConnectUiEvent.Confirm -> {
          val confirm = content as? ConnectContent.Confirm
          if (link == null || confirm == null || confirm.isSaving) return@ConnectUiState
          val server = existingServer?.server
          if (server == null) {
            // The sign-in screen takes the link from here, and discards it once signed in
            navigator.pop()
            navigator.goTo(
              LoginScreen.Connect(
                serverUrl = link.serverUrl,
                serverName = link.serverName.orEmpty(),
                linkId = screen.linkId,
              ),
            )
          } else {
            isSaving = true
            coroutineScope.launch {
              updateServiceToken(server, link)
              connectLinkInbox.discard(screen.linkId)
              val session = userSession
              if (session is UserSession.NeedsAuthentication && session.server.user.id == server.user.id) {
                // The sign-in form waiting underneath loaded the old headers; give it the new ones
                navigator.resetRoot(LoginScreen.ReAuthentication(session.server))
              } else {
                updatedServer = server
              }
            }
          }
        }
      }
    }
  }

  private suspend fun updateServiceToken(server: Server, link: ConnectLink) {
    val headers = accountManager.getExtraHeaders(server.user.id)
      ?.let { NetworkSettings(extraHeaders = it) }
      .withCloudflareServiceToken(link.clientId, link.clientSecret)
      .extraHeaders
    accountManager.setExtraHeaders(server.user.id, headers)
  }

  /** The result of looking for an account on the link's server: [server] is `null` when there's none. */
  private data class ExistingServer(val server: Server?)
}
