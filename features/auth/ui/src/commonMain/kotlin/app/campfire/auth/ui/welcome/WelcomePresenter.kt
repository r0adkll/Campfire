// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.welcome

import androidx.compose.runtime.Composable
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.ui.login.LoginPresenter
import app.campfire.common.screens.LoginScreen
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.UserScope
import app.campfire.core.permission.LocalNetworkPermissionController
import app.campfire.network.oidc.AuthorizationFlow
import app.campfire.ui.theming.api.AppThemeRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(WelcomeScreen::class, UserScope::class)
@Inject
class WelcomePresenter(
  private val authRepository: AuthRepository,
  private val oauthAuthorizationFlow: AuthorizationFlow,
  private val localNetworkPermission: LocalNetworkPermissionController,
  private val appThemeRepository: AppThemeRepository,
  @Assisted private val navigator: Navigator,
) : Presenter<WelcomeUiState> {

  private val loginPresenter = LoginPresenter(
    screen = LoginScreen.New,
    navigator = navigator,
    authRepository = authRepository,
    oauthAuthorizationFlow = oauthAuthorizationFlow,
    localNetworkPermission = localNetworkPermission,
    appThemeRepository = appThemeRepository,
  )

  @Composable
  override fun present(): WelcomeUiState {
    val loginUiState = loginPresenter.present()

    return WelcomeUiState(
      loginUiState = loginUiState,
    ) { event ->
      when (event) {
        WelcomeUiEvent.AddCampsite -> navigator.goTo(LoginScreen.New)
      }
    }
  }
}
