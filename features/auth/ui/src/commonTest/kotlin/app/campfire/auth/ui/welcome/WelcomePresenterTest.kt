// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.welcome

import app.campfire.account.api.RestorableAccount
import app.campfire.auth.ui.login.FakeAuthRepository
import app.campfire.auth.ui.login.FakeRestorableAccountRepository
import app.campfire.auth.ui.login.GrantedLocalNetworkPermission
import app.campfire.auth.ui.login.RecordingAppThemeRepository
import app.campfire.auth.ui.login.UnusedAuthorizationFlow
import app.campfire.common.screens.LoginScreen
import app.campfire.common.screens.WelcomeScreen
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class WelcomePresenterTest {

  private val navigator = FakeNavigator(WelcomeScreen)

  private val presenter = WelcomePresenter(
    authRepository = FakeAuthRepository(),
    oauthAuthorizationFlow = UnusedAuthorizationFlow(),
    localNetworkPermission = GrantedLocalNetworkPermission(),
    appThemeRepository = RecordingAppThemeRepository(),
    restorableAccountRepository = FakeRestorableAccountRepository(listOf(ACCOUNT)),
    navigator = navigator,
  )

  @Test
  fun `picking a restored account opens its login`() = runTest {
    presenter.test {
      var state = awaitItem()
      while (state.loginUiState.restorableAccounts.isEmpty()) state = awaitItem()
      // Fakes configured for this machine's builds come after the real accounts
      assertThat(state.loginUiState.restorableAccounts.first()).isEqualTo(ACCOUNT)

      state.eventSink(WelcomeUiEvent.RestoreAccount(ACCOUNT))
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(navigator.awaitNextScreen()).isEqualTo(
      LoginScreen.Restore(
        serverUrl = ACCOUNT.serverUrl,
        serverName = ACCOUNT.serverName,
        userName = ACCOUNT.userName,
      ),
    )
  }

  private companion object {
    val ACCOUNT = RestorableAccount(
      serverUrl = "https://abs.example.com",
      serverName = "Home",
      userId = "user-alice",
      userName = "alice",
    )
  }
}
