package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.LibraryRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Inject
class LibraryItemPresenter(
  @Assisted private val screen: LibraryItemScreen,
  @Assisted private val navigator: Navigator,
  private val repository: LibraryItemRepository,
) : Presenter<LibraryItemUiState> {

  @Composable
  override fun present(): LibraryItemUiState {
    val libraryItemContentState by remember {
      repository.observeLibraryItem(screen.libraryItemId)
        .map { LibraryItemContentState.Loaded(it) }
        .catch { LibraryItemContentState.Error }
    }.collectAsState(LibraryItemContentState.Loading)

    return LibraryItemUiState(
      libraryItemContentState = libraryItemContentState,
    ) { event ->
      when (event) {
        LibraryItemUiEvent.OnBack -> navigator.pop()
      }
    }
  }
}
