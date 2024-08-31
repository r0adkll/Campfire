package app.campfire.author.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.author.api.AuthorRepository
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.AuthorsScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(AuthorsScreen::class, UserScope::class)
@Inject
class AuthorsPresenter(
  @Assisted private val navigator: Navigator,
  private val authorRepository: AuthorRepository,
) : Presenter<AuthorsUiState> {

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
        is AuthorsUiEvent.AuthorClick -> navigator.goTo(AuthorDetailScreen(event.author.id))
      }
    }
  }
}
