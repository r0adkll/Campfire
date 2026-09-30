// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.welcome

import app.campfire.account.api.BackedUpAccountRestorer
import app.campfire.account.api.RestorableAccount
import app.campfire.auth.ui.login.FakeAuthRepository
import app.campfire.auth.ui.login.FakePasswordCredentials
import app.campfire.auth.ui.login.FakeRestorableAccountRepository
import app.campfire.auth.ui.login.GrantedLocalNetworkPermission
import app.campfire.auth.ui.login.RecordingAppThemeRepository
import app.campfire.auth.ui.login.UnusedAuthorizationFlow
import app.campfire.common.screens.LoginScreen
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.model.UserId
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class WelcomePresenterTest {

  private val navigator = FakeNavigator(WelcomeScreen)
  private val restorer = FakeBackedUpAccountRestorer()

  private val presenter = WelcomePresenter(
    authRepository = FakeAuthRepository(),
    oauthAuthorizationFlow = UnusedAuthorizationFlow(),
    localNetworkPermission = GrantedLocalNetworkPermission(),
    appThemeRepository = RecordingAppThemeRepository(),
    restorableAccountRepository = FakeRestorableAccountRepository(listOf(ACCOUNT)),
    passwordCredentials = FakePasswordCredentials(),
    backedUpAccountRestorer = restorer,
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

  @Test
  fun `opening welcome signs kept accounts back in and shows which`() = runTest {
    presenter.test {
      restorer.restoringUsers.value = setOf(ACCOUNT.userId)

      val state = awaitItemMatching { it.restoringUserIds.isNotEmpty() }
      assertThat(state.restoringUserIds).isEqualTo(setOf(ACCOUNT.userId))
      assertThat(restorer.restoreCalls).isEqualTo(1)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private class FakeBackedUpAccountRestorer : BackedUpAccountRestorer {
    val restoringUsers = MutableStateFlow<Set<UserId>>(emptySet())
    var restoreCalls = 0
    override val restoring: Flow<Set<UserId>> = restoringUsers
    override fun restore() {
      restoreCalls++
    }
  }

  private suspend inline fun ReceiveTurbine<WelcomeUiState>.awaitItemMatching(
    predicate: (WelcomeUiState) -> Boolean,
  ): WelcomeUiState {
    while (true) {
      val item = awaitItem()
      if (predicate(item)) return item
    }
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
