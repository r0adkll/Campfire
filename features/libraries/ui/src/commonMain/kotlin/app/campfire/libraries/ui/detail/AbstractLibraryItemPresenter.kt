package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import com.slack.circuit.runtime.Navigator

interface AbstractLibraryItemPresenter {

  @Composable
  fun present(
    screen: LibraryItemScreen,
    navigator: Navigator,
    libraryItem: LibraryItem,
  ): ContentUiState
}
