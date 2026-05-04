package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import com.slack.circuit.runtime.Navigator
import me.tatarka.inject.annotations.Assisted

interface AbstractLibraryItemPresenter  {

  @Composable
  fun present(
    screen: LibraryItemScreen,
    navigator: Navigator,
    libraryItem: LibraryItem,
  ) : ContentUiState
}
