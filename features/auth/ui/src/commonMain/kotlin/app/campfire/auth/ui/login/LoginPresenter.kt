package app.campfire.auth.ui.login

import androidx.compose.runtime.Composable
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.MergeActivityScope
import com.r0adkll.kotlininject.merge.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(MergeActivityScope::class, LoginScreen::class)
@Inject
class LoginPresenter(
  @Assisted private val navigator: Navigator,
) : Presenter<LoginUiState> {

  @Composable
  override fun present(): LoginUiState {

    return LoginUiState { event ->
      TODO("Handle events")
    }
  }
}
