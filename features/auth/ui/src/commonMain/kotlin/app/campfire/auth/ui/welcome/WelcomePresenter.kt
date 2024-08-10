package app.campfire.auth.ui.welcome

import androidx.compose.runtime.Composable
import app.campfire.common.screens.LoginScreen
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.ActivityScope
import com.r0adkll.kotlininject.merge.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(ActivityScope::class, WelcomeScreen::class)
@Inject
class WelcomePresenter(
  @Assisted private val navigator: Navigator,
) : Presenter<WelcomeUiState> {

  @Composable
  override fun present(): WelcomeUiState {
    return WelcomeUiState { event ->
      when (event) {
        WelcomeUiEvent.AddCampsite -> navigator.goTo(LoginScreen)
      }
    }
  }
}
