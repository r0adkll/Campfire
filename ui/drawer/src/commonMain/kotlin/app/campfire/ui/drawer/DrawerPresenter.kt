package app.campfire.ui.drawer

import androidx.compose.runtime.Composable
import app.campfire.common.screens.DrawerScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(DrawerScreen::class, UserScope::class)
@Inject
class DrawerPresenter(
  @Assisted private val navigator: Navigator,
) : Presenter<DrawerUiState> {

  @Composable
  override fun present(): DrawerUiState {

    return DrawerUiState { event ->
      when (event) {
        is DrawerUiEvent.ItemClick -> navigator.goTo(event.item.screen)
      }
    }
  }
}
