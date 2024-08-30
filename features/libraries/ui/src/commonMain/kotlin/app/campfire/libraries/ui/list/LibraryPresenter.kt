package app.campfire.libraries.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.common.screens.LibraryScreen
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.di.UserScope
import app.campfire.core.settings.ItemDisplayState
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
  private val settings: CampfireSettings,
) : Presenter<LibraryUiState> {

  @Composable
  override fun present(): LibraryUiState {
    val contentState by remember {
      repository.observeLibraryItems()
        .map { LibraryContentState.Loaded(it) }
        .catch { LibraryContentState.Error }
    }.collectAsState(LibraryContentState.Loading)

    val itemDisplayState by settings.observeLibraryItemDisplayState()
      .collectAsState(ItemDisplayState.List)

    // TODO: Keep filter state and filter the above loaded content

    return LibraryUiState(
      contentState = contentState,
      itemDisplayState = itemDisplayState,
    ) { event ->
      when (event) {
        LibraryUiEvent.OpenSearch -> {
          // TODO: Open search overlay screen
        }
        LibraryUiEvent.ToggleFilter -> {
          // TODO: Navigate to bottom sheet filter, with result
        }
        LibraryUiEvent.ToggleItemDisplayState -> {
          settings.libraryItemDisplayState = when (itemDisplayState) {
            ItemDisplayState.List -> ItemDisplayState.Grid
            ItemDisplayState.Grid -> ItemDisplayState.List
          }
        }
        is LibraryUiEvent.ItemClick -> navigator.goTo(LibraryItemScreen(event.libraryItem.id))
      }
    }
  }
}
