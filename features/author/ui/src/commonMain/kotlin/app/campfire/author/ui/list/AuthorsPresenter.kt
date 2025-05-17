package app.campfire.author.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.author.api.AuthorRepository
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.AuthorsScreen
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
class AuthorsPresenter(
  @Assisted private val navigator: Navigator,
  private val authorRepository: AuthorRepository,
) : Presenter<AuthorsUiState> {

  @CircuitInject(AuthorsScreen::class, UserScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): AuthorsPresenter
  }

  @Composable
  override fun present(): AuthorsUiState {
    val authorContentState by remember {
      authorRepository.observeAuthors()
        .map { AuthorsContentState.Loaded(it) }
        .catch { AuthorsContentState.Error }
    }.collectAsState(AuthorsContentState.Loading)

    return AuthorsUiState(
      authorContentState = authorContentState,
    ) { event ->
      when (event) {
        is AuthorsUiEvent.AuthorClick -> navigator.goTo(AuthorDetailScreen(event.author.id, event.author.name))
      }
    }
  }
}
