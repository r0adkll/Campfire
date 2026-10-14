// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import app.campfire.account.api.RestorableAccount
import app.campfire.auth.api.model.AUTH_METHOD_LOCAL
import app.campfire.auth.api.model.AUTH_METHOD_OPENID
import app.campfire.auth.api.screen.ScanSetupCodeScreen
import app.campfire.common.screens.LoginScreen
import app.campfire.core.navigation.ConnectLink
import app.campfire.core.navigation.ConnectLinkResult
import app.campfire.network.oidc.AuthorizationFlow
import app.campfire.ui.theming.api.AppTheme
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class LoginPresenterTest {

  private val restorableAccounts = FakeRestorableAccountRepository(listOf(ALICE, BOB))
  private var authRepository = FakeAuthRepository()
  private var passwordCredentials = FakePasswordCredentials()
  private val appThemeRepository = RecordingAppThemeRepository()
  private var authorizationFlow: AuthorizationFlow = UnusedAuthorizationFlow()
  private var connectLinkInbox = FakeConnectLinkInbox()
  private var qrCodeScanner = FakeQrCodeScanner(isSupported = false)

  @Test
  fun `restored accounts are offered and prefill the form`() = runTest {
    presenter(LoginScreen.Additional).test {
      val state = awaitItemMatching { it.restorableAccounts.isNotEmpty() }
      assertThat(state.restorableAccounts).containsExactly(ALICE, BOB)

      state.eventSink(LoginUiEvent.Password("stale"))
      awaitItemMatching { it.password == "stale" }
        .eventSink(LoginUiEvent.SelectRestorableAccount(BOB))

      val selected = awaitItemMatching { it.userName == BOB.userName && it.password.isEmpty() }
      assertThat(selected.serverUrl).isEqualTo(BOB.serverUrl)
      assertThat(selected.serverName).isEqualTo(BOB.serverName)
      assertThat(selected.password).isEqualTo("")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `dismissing a restored account stops offering it`() = runTest {
    presenter(LoginScreen.Additional).test {
      awaitItemMatching { it.restorableAccounts.isNotEmpty() }
        .eventSink(LoginUiEvent.DismissRestorableAccount(ALICE))

      val state = awaitItemMatching { it.restorableAccounts.size == 1 }
      assertThat(state.restorableAccounts).containsExactly(BOB)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `picking a restored account focuses the password until one is typed`() = runTest {
    presenter(LoginScreen.Additional).test {
      val state = awaitItemMatching { it.restorableAccounts.isNotEmpty() }
      assertThat(state.focusPassword).isFalse()

      state.eventSink(LoginUiEvent.SelectRestorableAccount(BOB))
      awaitItemMatching { it.focusPassword }
        .eventSink(LoginUiEvent.Password("s"))

      assertThat(awaitItemMatching { it.password == "s" }.focusPassword).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a restore screen focuses the password`() = runTest {
    presenter(RESTORE_BOB).test {
      assertThat(awaitItem().focusPassword).isTrue()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `re-authentication doesn't offer restored accounts`() = runTest {
    val screen = LoginScreen.ReAuthentication(
      userId = "user-carol",
      userName = "carol",
      serverName = "Home",
      serverUrl = "https://abs.example.com",
    )
    presenter(screen).test {
      assertThat(awaitItem().restorableAccounts).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a restore screen prefills its account`() = runTest {
    presenter(RESTORE_BOB).test {
      val state = awaitItem()
      assertThat(state.serverUrl).isEqualTo(BOB.serverUrl)
      assertThat(state.serverName).isEqualTo(BOB.serverName)
      assertThat(state.userName).isEqualTo(BOB.userName)
      assertThat(state.password).isEqualTo("")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `signing back into a restored account keeps the restored theme`() = runTest {
    signIn(RESTORE_BOB)

    assertThat(authRepository.authenticatedUserNames).containsExactly(BOB.userName)
    assertThat(appThemeRepository.appliedThemes).isEmpty()
  }

  @Test
  fun `signing into a picked restored account keeps the restored theme`() = runTest {
    presenter(LoginScreen.Additional).test {
      awaitItemMatching { it.restorableAccounts.isNotEmpty() }
        .eventSink(LoginUiEvent.SelectRestorableAccount(BOB))
      awaitItemMatching { it.userName == BOB.userName }
        .eventSink(LoginUiEvent.Password("secret"))
      awaitItemMatching { it.password == "secret" && it.connectionState is ConnectionState.Success }
        .eventSink(LoginUiEvent.AddCampsite)
      awaitItemMatching { it.isAuthenticating }
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(authRepository.authenticatedUserNames).containsExactly(BOB.userName)
    assertThat(appThemeRepository.appliedThemes).isEmpty()
  }

  @Test
  fun `a server behind Cloudflare Access asks for a service token`() = runTest {
    authRepository = FakeAuthRepository(accessClientId = "good.access")

    presenter(LoginScreen.Additional).test {
      awaitItem().eventSink(LoginUiEvent.ServerUrl(CAROL_URL))

      val protected = awaitItemMatching { it.connectionState is ConnectionState.AccessProtected }
      assertThat(protected.connectionState).isEqualTo(ConnectionState.AccessProtected(serviceTokenRejected = false))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a rejected service token is reported as rejected`() = runTest {
    authRepository = FakeAuthRepository(accessClientId = "good.access")

    presenter(LoginScreen.Additional).test {
      val state = awaitItem()
      state.eventSink(LoginUiEvent.ChangeNetworkSettings(null.withCloudflareServiceToken("wrong.access", "secret")))
      state.eventSink(LoginUiEvent.ServerUrl(CAROL_URL))

      val rejected = awaitItemMatching { it.connectionState is ConnectionState.AccessProtected }
      assertThat(rejected.connectionState).isEqualTo(ConnectionState.AccessProtected(serviceTokenRejected = true))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `adding the service token connects to the server`() = runTest {
    authRepository = FakeAuthRepository(accessClientId = "good.access")

    presenter(LoginScreen.Additional).test {
      awaitItem().eventSink(LoginUiEvent.ServerUrl(CAROL_URL))
      awaitItemMatching { it.connectionState is ConnectionState.AccessProtected }
        .eventSink(LoginUiEvent.ChangeNetworkSettings(null.withCloudflareServiceToken("good.access", "secret")))

      awaitItemMatching { it.connectionState is ConnectionState.Success }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a fresh sign in applies the picked theme`() = runTest {
    signIn(LoginScreen.Restore(serverUrl = CAROL_URL, serverName = "Home", userName = "carol"), fresh = true)

    assertThat(appThemeRepository.appliedThemes).containsExactly(AppTheme.Fixed.Tent)
  }

  /**
   * Sign in from [screen] with a password. With [fresh], the fields are typed into a blank
   * [LoginScreen.Additional] form instead, matching [screen]'s account.
   */
  private suspend fun signIn(screen: LoginScreen.Restore, fresh: Boolean = false) {
    presenter(if (fresh) LoginScreen.Additional else screen).test {
      if (fresh) {
        val state = awaitItem()
        state.eventSink(LoginUiEvent.ServerUrl(screen.serverUrl))
        state.eventSink(LoginUiEvent.UserName(screen.userName))
      }
      awaitItemMatching { it.userName == screen.userName }
        .eventSink(LoginUiEvent.Password("secret"))
      awaitItemMatching { it.password == "secret" && it.connectionState is ConnectionState.Success }
        .eventSink(LoginUiEvent.AddCampsite)
      awaitItemMatching { it.isAuthenticating }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `signing in to a server that already has an account names that account`() = runTest {
    authRepository = FakeAuthRepository(addedServers = mapOf(CAROL_URL to "alice"))

    presenter(LoginScreen.Additional).test {
      val state = awaitItem()
      state.eventSink(LoginUiEvent.ServerUrl(CAROL_URL))
      state.eventSink(LoginUiEvent.UserName("carol"))
      awaitItemMatching { it.userName == "carol" }
        .eventSink(LoginUiEvent.Password("secret"))
      awaitItemMatching { it.password == "secret" && it.connectionState is ConnectionState.Success }
        .eventSink(LoginUiEvent.AddCampsite)

      val failed = awaitItemMatching { it.authError != null }
      assertThat(failed.authError).isEqualTo(AuthError.ServerAlreadyAdded("alice"))
      assertThat(failed.isAuthenticating).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
    assertThat(authRepository.authenticatedUserNames).isEmpty()
    assertThat(passwordCredentials.offeredToSave).isEmpty()
  }

  @Test
  fun `OpenID sign in to a server that already has an account stops before the provider`() = runTest {
    authRepository = FakeAuthRepository(
      authMethods = listOf(AUTH_METHOD_OPENID),
      addedServers = mapOf(CAROL_URL to "alice"),
    )

    // The authorization flow fails the test if it's reached
    presenter(LoginScreen.Additional).test {
      awaitItem().eventSink(LoginUiEvent.ServerUrl(CAROL_URL))
      awaitItemMatching { it.connectionState is ConnectionState.Success }
        .eventSink(LoginUiEvent.StartOpenIdAuth)

      assertThat(awaitItemMatching { it.authError != null }.authError)
        .isEqualTo(AuthError.ServerAlreadyAdded("alice"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a typed password is offered to the password manager after signing in`() = runTest {
    signIn(RESTORE_BOB)

    assertThat(passwordCredentials.offeredToSave)
      .containsExactly(Triple(BOB.serverUrl, BOB.userName, "secret"))
  }

  @Test
  fun `a saved password signs a restored account straight back in`() = runTest {
    passwordCredentials = FakePasswordCredentials(mapOf((BOB.serverUrl to BOB.userName) to "saved"))

    presenter(RESTORE_BOB).test {
      awaitItemMatching { it.isAuthenticating }
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(authRepository.authenticatedPasswords).containsExactly("saved")
    // It's already in the password manager
    assertThat(passwordCredentials.offeredToSave).isEmpty()
  }

  @Test
  fun `picking a restored account uses its saved password`() = runTest {
    passwordCredentials = FakePasswordCredentials(mapOf((BOB.serverUrl to BOB.userName) to "saved"))

    presenter(LoginScreen.Additional).test {
      awaitItemMatching { it.restorableAccounts.isNotEmpty() }
        .eventSink(LoginUiEvent.SelectRestorableAccount(BOB))
      awaitItemMatching { it.isAuthenticating }
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(authRepository.authenticatedPasswords).containsExactly("saved")
  }

  @Test
  fun `re-authentication uses the saved password`() = runTest {
    passwordCredentials = FakePasswordCredentials(mapOf((BOB.serverUrl to BOB.userName) to "saved"))
    val screen = LoginScreen.ReAuthentication(
      userId = BOB.userId,
      userName = BOB.userName,
      serverName = BOB.serverName,
      serverUrl = BOB.serverUrl,
    )

    presenter(screen).test {
      awaitItemMatching { it.isAuthenticating }
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(authRepository.authenticatedPasswords).containsExactly("saved")
  }

  @Test
  fun `without a saved password the form waits for one to be typed`() = runTest {
    presenter(RESTORE_BOB).test {
      val found = awaitItemMatching { it.connectionState is ConnectionState.Success }
      testScheduler.advanceUntilIdle()
      assertThat(found.password).isEqualTo("")
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(passwordCredentials.lookups).containsExactly(BOB.serverUrl to BOB.userName)
    assertThat(authRepository.authenticatedUserNames).isEmpty()
  }

  @Test
  fun `the password manager is asked once per account`() = runTest {
    presenter(RESTORE_BOB).test {
      val found = awaitItemMatching { it.connectionState is ConnectionState.Success }
      // Leave the server and come back to it
      found.eventSink(LoginUiEvent.ServerUrl("https://elsewhere.example.com"))
      awaitItemMatching { it.serverUrl == "https://elsewhere.example.com" }
        .eventSink(LoginUiEvent.ServerUrl(BOB.serverUrl))
      awaitItemMatching { it.serverUrl == BOB.serverUrl && it.connectionState is ConnectionState.Success }
      cancelAndIgnoreRemainingEvents()
    }

    // The other server is another account, so it gets its own lookup
    assertThat(passwordCredentials.lookups.count { it == BOB.serverUrl to BOB.userName }).isEqualTo(1)
  }

  @Test
  fun `a new account doesn't ask the password manager`() = runTest {
    signIn(LoginScreen.Restore(serverUrl = CAROL_URL, serverName = "Home", userName = "carol"), fresh = true)

    assertThat(passwordCredentials.lookups).isEmpty()
  }

  @Test
  fun `a server without password sign in doesn't ask the password manager`() = runTest {
    authRepository = FakeAuthRepository(authMethods = listOf(AUTH_METHOD_OPENID))
    passwordCredentials = FakePasswordCredentials(mapOf((BOB.serverUrl to BOB.userName) to "saved"))

    presenter(RESTORE_BOB).test {
      awaitItemMatching { it.connectionState is ConnectionState.Success }
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(passwordCredentials.lookups).isEmpty()
  }

  @Test
  fun `fake restorable accounts follow the real ones`() = runTest {
    val presenter = presenter(LoginScreen.Additional).apply { fakeRestorableAccountCount = 7 }
    presenter.test {
      val accounts = awaitItemMatching { it.restorableAccounts.isNotEmpty() }.restorableAccounts
      assertThat(accounts.take(2)).containsExactly(ALICE, BOB)
      assertThat(accounts.size).isEqualTo(9)
      // Each needs its own key in the row
      assertThat(accounts.map { it.userId + it.serverUrl }.toSet().size).isEqualTo(9)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a setup link prefills the server and its service token`() = runTest {
    authRepository = FakeAuthRepository(accessClientId = LINK_CLIENT_ID)
    connectLinkInbox = FakeConnectLinkInbox(LINK_ID to ConnectLinkResult.Valid(carolLink()))

    presenter(CONNECT_CAROL).test {
      val state = awaitItemMatching { it.connectionState is ConnectionState.Success }
      assertThat(state.serverUrl).isEqualTo(CAROL_URL)
      assertThat(state.serverName).isEqualTo("Carol's Books")
      assertThat(state.networkSettings).isEqualTo(null.withCloudflareServiceToken(LINK_CLIENT_ID, LINK_CLIENT_SECRET))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a setup link asking for OpenID starts it once the server answers`() = runTest {
    authRepository = FakeAuthRepository(authMethods = listOf(AUTH_METHOD_OPENID), accessClientId = LINK_CLIENT_ID)
    val flow = RecordingAuthorizationFlow()
    authorizationFlow = flow
    connectLinkInbox = FakeConnectLinkInbox(LINK_ID to ConnectLinkResult.Valid(carolLink(openId = true)))

    presenter(CONNECT_CAROL).test {
      // The provider was left without signing in, so the user is back on the form
      assertThat(awaitItemMatching { it.authError != null }.authError).isEqualTo(AuthError.OAuthError)
      testScheduler.advanceUntilIdle()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(flow.requests).containsExactly(
      CAROL_URL to null.withCloudflareServiceToken(LINK_CLIENT_ID, LINK_CLIENT_SECRET).extraHeaders,
    )
    // Still needed if the user tries again
    assertThat(connectLinkInbox.links.keys).containsOnly(LINK_ID)
  }

  @Test
  fun `a setup link without OpenID waits on the form`() = runTest {
    authRepository = FakeAuthRepository(authMethods = listOf(AUTH_METHOD_LOCAL, AUTH_METHOD_OPENID))
    val flow = RecordingAuthorizationFlow()
    authorizationFlow = flow
    connectLinkInbox = FakeConnectLinkInbox(LINK_ID to ConnectLinkResult.Valid(carolLink(openId = false)))

    presenter(CONNECT_CAROL).test {
      val state = awaitItemMatching { it.connectionState is ConnectionState.Success }
      testScheduler.advanceUntilIdle()
      assertThat(state.isAuthenticating).isFalse()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(flow.requests).isEmpty()
  }

  @Test
  fun `a setup link that's gone leaves the server for a token to be added`() = runTest {
    authRepository = FakeAuthRepository(accessClientId = LINK_CLIENT_ID)

    presenter(CONNECT_CAROL).test {
      val state = awaitItemMatching { it.connectionState is ConnectionState.AccessProtected }
      assertThat(state.serverUrl).isEqualTo(CAROL_URL)
      assertThat(state.networkSettings).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `signing in from a setup link lets go of it`() = runTest {
    authRepository = FakeAuthRepository(accessClientId = LINK_CLIENT_ID)
    connectLinkInbox = FakeConnectLinkInbox(LINK_ID to ConnectLinkResult.Valid(carolLink()))

    presenter(CONNECT_CAROL).test {
      val state = awaitItem()
      state.eventSink(LoginUiEvent.UserName("carol"))
      state.eventSink(LoginUiEvent.Password("secret"))
      awaitItemMatching { it.password == "secret" && it.connectionState is ConnectionState.Success }
        .eventSink(LoginUiEvent.AddCampsite)
      awaitItemMatching { it.isAuthenticating }
      testScheduler.advanceUntilIdle()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(authRepository.authenticatedUserNames).containsExactly("carol")
    assertThat(connectLinkInbox.links).isEmpty()
  }

  @Test
  fun `backing out of a setup link lets go of it`() = runTest {
    connectLinkInbox = FakeConnectLinkInbox(LINK_ID to ConnectLinkResult.Valid(carolLink()))

    presenter(CONNECT_CAROL).test {
      awaitItem().eventSink(LoginUiEvent.NavigateBack)
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(connectLinkInbox.links).isEmpty()
  }

  @Test
  fun `scanning a setup code is offered only where the device can scan`() = runTest {
    presenter(LoginScreen.Additional).test {
      assertThat(awaitItem().canScanSetupCode).isFalse()
      cancelAndIgnoreRemainingEvents()
    }

    qrCodeScanner = FakeQrCodeScanner(isSupported = true)
    presenter(LoginScreen.Additional).test {
      assertThat(awaitItem().canScanSetupCode).isTrue()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `scan setup code opens the scanner`() = runTest {
    qrCodeScanner = FakeQrCodeScanner(isSupported = true)
    val navigator = FakeNavigator(LoginScreen.Additional)

    presenter(LoginScreen.Additional, navigator).test {
      awaitItem().eventSink(LoginUiEvent.ScanSetupCode)
      assertThat(navigator.awaitNextScreen()).isEqualTo(ScanSetupCodeScreen)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun presenter(
    screen: LoginScreen,
    navigator: FakeNavigator = FakeNavigator(screen),
  ) = LoginPresenter(
    screen = screen,
    navigator = navigator,
    authRepository = authRepository,
    oauthAuthorizationFlow = authorizationFlow,
    localNetworkPermission = GrantedLocalNetworkPermission(),
    appThemeRepository = appThemeRepository,
    restorableAccountRepository = restorableAccounts,
    passwordCredentials = passwordCredentials,
    connectLinkInbox = connectLinkInbox,
    qrCodeScanner = qrCodeScanner,
  ).apply {
    // Ignore any fakes configured for this machine's builds
    fakeRestorableAccountCount = 0
  }

  private companion object {
    const val CAROL_URL = "https://carol.example.net"
    const val LINK_ID = 42L
    const val LINK_CLIENT_ID = "0123456789abcdef.access"
    const val LINK_CLIENT_SECRET = "0123456789abcdef0123456789abcdef"
    val CONNECT_CAROL = LoginScreen.Connect(serverUrl = CAROL_URL, serverName = "Carol's Books", linkId = LINK_ID)

    fun carolLink(openId: Boolean = false) = ConnectLink(
      serverUrl = CAROL_URL,
      serverName = "Carol's Books",
      clientId = LINK_CLIENT_ID,
      clientSecret = LINK_CLIENT_SECRET,
      signInWithOpenId = openId,
    )

    val ALICE = RestorableAccount(
      serverUrl = "https://abs.example.com",
      serverName = "Home",
      userId = "user-alice",
      userName = "alice",
    )
    val BOB = RestorableAccount(
      serverUrl = "https://books.example.org",
      serverName = "Library",
      userId = "user-bob",
      userName = "bob",
    )
    val RESTORE_BOB = LoginScreen.Restore(
      serverUrl = BOB.serverUrl,
      serverName = BOB.serverName,
      userName = BOB.userName,
    )
  }
}

private suspend inline fun ReceiveTurbine<LoginUiState>.awaitItemMatching(
  predicate: (LoginUiState) -> Boolean,
): LoginUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}
