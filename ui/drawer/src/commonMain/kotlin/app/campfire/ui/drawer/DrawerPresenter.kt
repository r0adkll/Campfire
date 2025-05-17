package app.campfire.ui.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.account.api.AccountManager
import app.campfire.common.screens.DrawerScreen
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.UserScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
class DrawerPresenter(
  private val accountManager: AccountManager,
  @Assisted private val navigator: Navigator,
) : Presenter<DrawerUiState> {

  @CircuitInject(DrawerScreen::class, UserScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): DrawerPresenter
  }

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

        DrawerUiEvent.AddAccount -> navigator.goTo(LoginScreen(isAddingAccount = true))
      }
    }
  }
}
