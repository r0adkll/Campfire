package app.campfire.author.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.author.api.AuthorRepository
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.core.di.UserScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Inject
class AuthorDetailPresenter(
  @Assisted private val screen: AuthorDetailScreen,
  @Assisted private val navigator: Navigator,
  private val authorRepository: AuthorRepository,
) : Presenter<AuthorDetailUiState> {

  @CircuitInject(AuthorDetailScreen::class, UserScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: AuthorDetailScreen, navigator: Navigator): AuthorDetailPresenter
  }

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
