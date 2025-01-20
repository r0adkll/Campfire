package app.campfire.account.ui.switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.account.api.ServerRepository
import app.campfire.core.coroutines.LoadState
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

typealias AccountSwitcherPresenterFactory = () -> AccountSwitcherPresenter

@Inject
class AccountSwitcherPresenter(
  private val serverRepository: ServerRepository,
) : Presenter<AccountSwitcherUiState> {

  @Composable
  override fun present(): AccountSwitcherUiState {
    val accountState by remember {
      serverRepository.observeCurrentServer()
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    val allAccounts by remember {
      serverRepository.observeAllServers()
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    return AccountSwitcherUiState(
      currentAccount = accountState,
      allAccounts = allAccounts,
    ) { event ->
    }
  }
}
