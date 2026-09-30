// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import app.campfire.account.api.RestorableAccount
import app.campfire.auth.api.model.AUTH_METHOD_OPENID
import app.campfire.common.screens.LoginScreen
import app.campfire.ui.theming.api.AppTheme
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
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

  private fun presenter(screen: LoginScreen) = LoginPresenter(
    screen = screen,
    navigator = FakeNavigator(screen),
    authRepository = authRepository,
    oauthAuthorizationFlow = UnusedAuthorizationFlow(),
    localNetworkPermission = GrantedLocalNetworkPermission(),
    appThemeRepository = appThemeRepository,
    restorableAccountRepository = restorableAccounts,
    passwordCredentials = passwordCredentials,
  ).apply {
    // Ignore any fakes configured for this machine's builds
    fakeRestorableAccountCount = 0
  }

  private companion object {
    const val CAROL_URL = "https://carol.example.net"

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
