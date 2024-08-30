package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Inject
class LibraryItemPresenter(
  @Assisted private val screen: LibraryItemScreen,
  @Assisted private val navigator: Navigator,
) : Presenter<LibraryItemUiState> {

  @Composable
  override fun present(): LibraryItemUiState {
    TODO("Not yet implemented")
  }
}
