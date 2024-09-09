package app.campfire.author.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.author.api.AuthorRepository
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(AuthorDetailScreen::class, UserScope::class)
@Inject
class AuthorDetailPresenter(
  @Assisted private val screen: AuthorDetailScreen,
  @Assisted private val navigator: Navigator,
  private val authorRepository: AuthorRepository,
) : Presenter<AuthorDetailUiState> {

  @Composable
  override fun present(): AuthorDetailUiState {
    val authorContentState by remember {
      authorRepository.observeAuthor(screen.authorId)
        .map { AuthorContentState.Loaded(it) }
        .catch { AuthorContentState.Error }
    }.collectAsState(AuthorContentState.Loading)

    return AuthorDetailUiState(
      authorContentState = authorContentState,
    ) { event ->
      when (event) {
        is AuthorDetailUiEvent.LibraryItemClick -> navigator.goTo(LibraryItemScreen(event.libraryItem.id))
        AuthorDetailUiEvent.Back -> navigator.pop()
      }
    }
  }
}
