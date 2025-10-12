package app.campfire.account.ui.switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.account.api.ServerRepository
import app.campfire.core.coroutines.LoadState
import app.campfire.libraries.api.LibraryRepository
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

typealias AccountSwitcherPresenterFactory = () -> AccountSwitcherPresenter

@Inject
class AccountSwitcherPresenter(
  private val serverRepository: ServerRepository,
  private val libraryRepository: LibraryRepository,
) : Presenter<AccountSwitcherUiState> {

  @Composable
  override fun present(): AccountSwitcherUiState {
    val scope = rememberCoroutineScope()

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

    val currentLibrary by remember {
      libraryRepository.observeCurrentLibrary()
        .catch { null }
    }.collectAsState(null)

    val allLibraries by remember {
      libraryRepository.observeAllLibraries()
        .map { it.sortedBy { library -> library.displayOrder } }
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    return AccountSwitcherUiState(
      currentAccount = accountState,
      libraryState = currentLibrary?.let {
        LibraryState(
          currentLibrary = it,
          allLibraries = allLibraries,
        )
      },
      allAccounts = allAccounts,
    ) { event ->
      when (event) {
        is AccountSwitcherUiEvent.SelectLibrary -> {
          scope.launch {
            libraryRepository.setCurrentLibrary(event.library)
          }
        }
      }
    }
  }
}
