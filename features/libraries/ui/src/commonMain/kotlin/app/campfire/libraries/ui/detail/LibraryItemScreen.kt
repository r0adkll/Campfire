package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject

@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Composable
fun LibraryItem(
  state: LibraryItemUiState,
  modifier: Modifier = Modifier,
) {
  TODO("Add screen compose code")
}
