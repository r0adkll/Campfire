package app.campfire.auth.ui.welcome

import androidx.compose.runtime.Composable
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.ui.login.LoginPresenter
import app.campfire.common.screens.LoginScreen
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.UserScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject

@Inject
class WelcomePresenter(
  private val authRepository: AuthRepository,
  @Assisted private val navigator: Navigator,
) : Presenter<WelcomeUiState> {

  @CircuitInject(WelcomeScreen::class, UserScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): WelcomePresenter
  }

  private val loginPresenter = LoginPresenter(
    navigator = navigator,
    authRepository = authRepository,
  )

  @Composable
  override fun present(): WelcomeUiState {
    val loginUiState = loginPresenter.present()

    return WelcomeUiState(
      loginUiState = loginUiState,
    ) { event ->
      when (event) {
        WelcomeUiEvent.AddCampsite -> navigator.goTo(LoginScreen())
      }
    }
  }
}
