package app.campfire.ui.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.account.api.AccountManager
import app.campfire.common.screens.DrawerScreen
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(DrawerScreen::class, UserScope::class)
@Inject
class DrawerPresenter(
  private val accountManager: AccountManager,
  @Assisted private val navigator: Navigator,
) : Presenter<DrawerUiState> {

  @Composable
  override fun present(): DrawerUiState {
    val scope = rememberCoroutineScope()

    return DrawerUiState { event ->
      when (event) {
        is DrawerUiEvent.ItemClick -> navigator.goTo(event.item.screen)

        is DrawerUiEvent.SwitchAccount -> {
          scope.launch {
            accountManager.switchAccount(event.server.user)
          }
        }

        DrawerUiEvent.AddAccount -> navigator.goTo(LoginScreen)
      }
    }
  }
}
