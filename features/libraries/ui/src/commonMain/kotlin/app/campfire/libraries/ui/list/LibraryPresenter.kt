package app.campfire.libraries.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.LibraryScreen
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.LibraryRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(LibraryScreen::class, UserScope::class)
@Inject
class LibraryPresenter(
  @Assisted private val navigator: Navigator,
  private val repository: LibraryRepository,
) : Presenter<LibraryUiState> {

  @Composable
  override fun present(): LibraryUiState {
    val contentState by remember {
      repository.observeLibraryItems()
        .map { LibraryContentState.Loaded(it) }
        .catch { LibraryContentState.Error }
    }.collectAsState(LibraryContentState.Loading)

    // TODO: Keep filter state and filter the above loaded content

    return LibraryUiState(
      contentState = contentState,
    ) { event ->
      when (event) {
        LibraryUiEvent.OpenDrawer -> TODO()
        LibraryUiEvent.OpenSearch -> TODO()
      }
    }
  }
}
