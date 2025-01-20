package app.campfire.account.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.account.api.AccountManager
import app.campfire.account.api.ServerRepository
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Server
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

typealias AccountPickerPresenterFactory = () -> AccountPickerPresenter

@Inject
class AccountPickerPresenter(
  private val serverRepository: ServerRepository,
  private val accountManager: AccountManager,
//  @Assisted private val navigator: Navigator,
) : Presenter<AccountPickerUiState> {

  @Composable
  override fun present(): AccountPickerUiState {
    val scope = rememberCoroutineScope()

    val accountState by remember {
      combine(
        serverRepository.observeCurrentServer(),
        serverRepository.observeAllServers(),
      ) { current, all ->
        AccountState(
          current = current,
          all = all.sortedWith(ServerComparator(current)),
        )
      }.map {
        LoadState.Loaded(it)
      }.catch {
        LoadState.Error
      }
    }.collectAsState(LoadState.Loading)

    return AccountPickerUiState(
      accountState = accountState,
    ) { event ->
      when (event) {
        is AccountPickerUiEvent.Logout -> {
          scope.launch {
            accountManager.logout(event.server)
          }
        }
      }
    }
  }
}

private class ServerComparator(
  val current: Server,
) : Comparator<Server> {

  /**
   * Compares its two arguments for order. Returns zero if the arguments are equal,
   * a negative number if the first argument is less than the second, or a positive number
   * if the first argument is greater than the second.
   */
  override fun compare(a: Server, b: Server): Int {
    val isCurrentA = a.user.id == current.user.id
    val isCurrentB = b.user.id == current.user.id

    return when {
      isCurrentA -> -1
      isCurrentB -> 1
      else -> a.name.compareTo(b.name)
    }
  }
}
