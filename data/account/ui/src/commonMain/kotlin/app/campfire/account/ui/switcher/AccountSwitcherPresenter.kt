package app.campfire.account.ui.switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.UserRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import me.tatarka.inject.annotations.Inject

@Inject
class AccountSwitcherPresenter(
  private val serverRepository: ServerRepository,
  private val userRepository: UserRepository,
) {

  @Composable
  fun present(): AccountSwitcherState {
    val state by remember {
      combine(
        serverRepository.observeCurrentServer(),
        userRepository.observeCurrentUser(),
      ) { server, user ->
        AccountSwitcherState.Loaded(server, user)
      }.catch { AccountSwitcherState.Error }
    }.collectAsState(AccountSwitcherState.Loading)

    return state
  }
}
