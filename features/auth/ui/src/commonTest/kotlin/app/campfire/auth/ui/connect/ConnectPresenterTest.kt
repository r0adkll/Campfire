// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.connect

import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.api.ServerRepository
import app.campfire.auth.api.screen.ConnectScreen
import app.campfire.auth.ui.login.FakeConnectLinkInbox
import app.campfire.common.screens.LoginScreen
import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.navigation.ConnectLink
import app.campfire.core.navigation.ConnectLinkResult
import app.campfire.core.session.UserSession
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ConnectPresenterTest {

  private val screen = ConnectScreen(LINK_ID)
  private val navigator = FakeNavigator(screen)
  private var inbox = FakeConnectLinkInbox(LINK_ID to ConnectLinkResult.Valid(LINK))
  private var servers = FakeServerRepository()
  private val accountManager = FakeAccountManager()
  private var userSession: UserSession = UserSession.LoggedOut

  @Test
  fun `a new server is confirmed by its host before anything is saved`() = runTest {
    presenter().test {
      val content = awaitContent<ConnectContent.Confirm>()
      assertThat(content).isEqualTo(
        ConnectContent.Confirm(
          serverHost = "abs.firenation.app",
          serverName = "Fire Nation",
          existingUserName = null,
          signInWithOpenId = true,
        ),
      )
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(accountManager.setHeaders).isEmpty()
  }

  @Test
  fun `confirming a new server moves on to sign in with the link`() = runTest {
    presenter().test {
      awaitState<ConnectContent.Confirm>().eventSink(ConnectUiEvent.Confirm)
      navigator.awaitPop()
      assertThat(navigator.awaitNextScreen()).isEqualTo(
        LoginScreen.Connect(serverUrl = SERVER_URL, serverName = "Fire Nation", linkId = LINK_ID),
      )
      cancelAndIgnoreRemainingEvents()
    }

    // The sign-in screen still needs the token
    assertThat(inbox.links.keys).containsOnly(LINK_ID)
    assertThat(accountManager.setHeaders).isEmpty()
  }

  @Test
  fun `cancelling discards the link`() = runTest {
    presenter().test {
      awaitState<ConnectContent.Confirm>().eventSink(ConnectUiEvent.Dismiss)
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(inbox.links).isEmpty()
    assertThat(accountManager.setHeaders).isEmpty()
  }

  @Test
  fun `an account already on the server gets the new token instead`() = runTest {
    val alice = server(userId = "alice", url = "$SERVER_URL/")
    servers = FakeServerRepository(alice)
    accountManager.headers["alice"] = mapOf(
      "X-Other" to "kept",
      "cf-access-client-id" to "old.access",
      "CF-Access-Client-Secret" to "old-secret",
    )
    userSession = UserSession.LoggedIn(alice.user)

    presenter().test {
      val confirm = awaitState<ConnectContent.Confirm>()
      assertThat((confirm.content as ConnectContent.Confirm).existingUserName).isEqualTo("alice")
      confirm.eventSink(ConnectUiEvent.Confirm)

      val updated = awaitState<ConnectContent.Updated>()
      assertThat(updated.content)
        .isEqualTo(ConnectContent.Updated(serverHost = "abs.firenation.app", userName = "alice"))
      updated.eventSink(ConnectUiEvent.Dismiss)
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(accountManager.setHeaders).containsExactly(
      "alice" to mapOf(
        "X-Other" to "kept",
        "CF-Access-Client-Id" to CLIENT_ID,
        "CF-Access-Client-Secret" to CLIENT_SECRET,
      ),
    )
    assertThat(inbox.links).isEmpty()
  }

  @Test
  fun `an account waiting to sign back in reloads its sign in form with the new token`() = runTest {
    val alice = server(userId = "alice", url = SERVER_URL)
    servers = FakeServerRepository(alice)
    userSession = UserSession.NeedsAuthentication(alice)

    presenter().test {
      awaitState<ConnectContent.Confirm>().eventSink(ConnectUiEvent.Confirm)
      assertThat(navigator.awaitResetRoot().newRoot).isEqualTo(LoginScreen.ReAuthentication(alice))
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(accountManager.setHeaders.map { it.first }).containsExactly("alice")
  }

  @Test
  fun `an account on another server isn't touched`() = runTest {
    servers = FakeServerRepository(server(userId = "bob", url = "https://abs.example.com"))

    presenter().test {
      assertThat(awaitContent<ConnectContent.Confirm>().existingUserName).isEqualTo(null)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a link from a newer Campfire asks for an update`() = runTest {
    inbox = FakeConnectLinkInbox(LINK_ID to ConnectLinkResult.UnsupportedVersion)

    presenter().test {
      awaitState<ConnectContent.UnsupportedVersion>().eventSink(ConnectUiEvent.Dismiss)
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(inbox.links).isEmpty()
  }

  @Test
  fun `a broken link says so`() = runTest {
    inbox = FakeConnectLinkInbox(LINK_ID to ConnectLinkResult.Invalid(ConnectLinkResult.Reason.InvalidServer))

    presenter().test {
      awaitContent<ConnectContent.Invalid>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a link lost with the process closes the screen`() = runTest {
    inbox = FakeConnectLinkInbox()

    presenter().test {
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun presenter() = ConnectPresenter(
    screen = screen,
    navigator = navigator,
    connectLinkInbox = inbox,
    serverRepository = servers,
    accountManager = accountManager,
    userSession = userSession,
  )

  private class FakeServerRepository(vararg servers: Server) : ServerRepository {
    private val all = servers.toList()
    override fun observeCurrentServer(): Flow<Server> = emptyFlow()
    override fun observeAllServers(): Flow<List<Server>> = flowOf(all)
    override suspend fun getCurrentServer(): Server? = all.firstOrNull()
    override suspend fun getAllServers(): List<Server> = all
    override suspend fun changeName(newName: String) = Unit
    override suspend fun remove(server: Server) = Unit
  }

  private class FakeAccountManager : AccountManager {
    val headers = mutableMapOf<UserId, Map<String, String>>()
    val setHeaders = mutableListOf<Pair<UserId, Map<String, String>>>()

    override suspend fun getExtraHeaders(userId: UserId): Map<String, String>? = headers[userId]
    override suspend fun setExtraHeaders(userId: UserId, headers: Map<String, String>) {
      setHeaders += userId to headers
      this.headers[userId] = headers
    }

    override suspend fun addAccount(
      serverUrl: String,
      accessToken: String,
      refreshToken: String?,
      extraHeaders: Map<String, String>?,
      user: User,
      activate: Boolean,
    ) = error("Unused")
    override suspend fun invalidateAccount(user: User) = error("Unused")
    override suspend fun switchAccount(user: User) = error("Unused")
    override suspend fun logout(server: Server) = error("Unused")
    override suspend fun getToken(userId: UserId): AbsToken? = error("Unused")
    override suspend fun updateToken(userId: UserId, newToken: AbsToken) = error("Unused")
    override fun observeExtraHeaders(userId: UserId): Flow<Map<String, String>> = emptyFlow()
  }

  private companion object {
    const val LINK_ID = 7L
    const val SERVER_URL = "https://abs.firenation.app"
    const val CLIENT_ID = "0123456789abcdef.access"
    const val CLIENT_SECRET = "0123456789abcdef0123456789abcdef"

    val LINK = ConnectLink(
      serverUrl = SERVER_URL,
      serverName = "Fire Nation",
      clientId = CLIENT_ID,
      clientSecret = CLIENT_SECRET,
      signInWithOpenId = true,
    )

    fun server(userId: UserId, url: String) = Server(
      url = url,
      name = "Fire Nation",
      user = User(
        id = userId,
        name = userId,
        selectedLibraryId = "library",
        type = User.Type.User,
        isActive = true,
        isLocked = false,
        lastSeen = 0L,
        createdAt = 0L,
        permissions = User.Permissions(
          download = true,
          update = false,
          delete = false,
          upload = false,
          accessAllLibraries = true,
          accessAllTags = true,
          accessExplicitContent = true,
        ),
        serverUrl = url,
      ),
      settings = Server.Settings(
        scannerFindCovers = false,
        scannerCoverProvider = "",
        scannerParseSubtitle = false,
        scannerPreferMatchedMetadata = false,
        scannerDisableWatcher = false,
        storeCoverWithItem = false,
        storeMetadataWithItem = false,
        metadataFileFormat = "",
        rateLimitLoginRequests = 0,
        rateLimitLoginWindow = 0,
        backupSchedule = "",
        backupsToKeep = 0,
        maxBackupSize = 0,
        loggerDailyLogsToKeep = 0,
        loggerScannerLogsToKeep = 0,
        homeBookshelfView = 0,
        bookshelfView = 0,
        sortingIgnorePrefix = false,
        sortingPrefixes = emptyList(),
        chromecastEnabled = false,
        dateFormat = "",
        timeFormat = "",
        language = "",
        logLevel = 0,
        version = "",
      ),
    )
  }
}

/** Skips ahead to the first state showing [T]. */
private suspend inline fun <reified T : ConnectContent> ReceiveTurbine<ConnectUiState>.awaitState(): ConnectUiState {
  while (true) {
    val state = awaitItem()
    if (state.content is T) return state
  }
}

private suspend inline fun <reified T : ConnectContent> ReceiveTurbine<ConnectUiState>.awaitContent(): T =
  awaitState<T>().content as T
