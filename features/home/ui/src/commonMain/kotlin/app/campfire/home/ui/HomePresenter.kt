package app.campfire.home.ui

import androidx.compose.runtime.Composable
import app.campfire.common.screens.HomeScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(HomeScreen::class, UserScope::class)
@Inject
class HomePresenter(
  @Assisted private val navigator: Navigator,
) : Presenter<HomeUiState> {

  @Composable
  override fun present(): HomeUiState {

    return HomeUiState { event ->

    }
  }
}
