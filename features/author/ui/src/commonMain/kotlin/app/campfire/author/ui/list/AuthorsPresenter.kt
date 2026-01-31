package app.campfire.author.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.author.api.AuthorRepository
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.AuthorsScreen
import app.campfire.core.di.UserScope
import app.campfire.settings.api.CampfireSettings
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

internal const val INVALID_AUTHOR_COUNT = -1

@CircuitInject(AuthorsScreen::class, UserScope::class)
@Inject
class AuthorsPresenter(
  @Assisted private val navigator: Navigator,
  private val userRepository: UserRepository,
  private val authorRepository: AuthorRepository,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<AuthorsUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): AuthorsUiState {
    // Using a pager requires us to remember the coroutine scope passed the
    // composition of this pager / ui. We should remember it until this screen
    // leaves the back stack
    val scope = rememberRetainedCoroutineScope()

    val sortMode by remember {
      settings.observeAuthorsSortMode()
    }.collectAsState(settings.authorsSortMode)

    val sortDirection by remember {
      settings.observeAuthorsSortDirection()
    }.collectAsState(settings.authorsSortDirection)

    val currentUser by remember {
      userRepository.observeStatefulCurrentUser()
    }.collectAsState()

    val lazyPagingItems = rememberRetained(currentUser, sortMode, sortDirection) {
      authorRepository.createAuthorsPager(
        user = currentUser,
        sortMode = sortMode,
        sortDirection = sortDirection,
      ).flow.cachedIn(scope)
    }.collectAsLazyPagingItems()

    val authorCount by remember(sortMode, sortDirection) {
      authorRepository.observeFilteredAuthorsCount(
        sortMode = sortMode,
        sortDirection = sortDirection,
      ).map { count -> count ?: INVALID_AUTHOR_COUNT }
    }.collectAsState(INVALID_AUTHOR_COUNT)

    return AuthorsUiState(
      lazyPagingItems = lazyPagingItems,
      numAuthors = authorCount,
      sortMode = sortMode,
      sortDirection = sortDirection,
    ) { event ->
      when (event) {
        is AuthorsUiEvent.AuthorClick -> {
          analytics.send(ContentSelected(ContentType.Author))
          navigator.goTo(AuthorDetailScreen(event.author.id, event.author.name))
        }

        is AuthorsUiEvent.SortModeSelected -> {
          analytics.send(ActionEvent("author_sort_mode", "selected", event.mode.storageKey))
          if (sortMode == event.mode) {
            settings.authorsSortDirection = sortDirection.flip()
          }
          settings.authorsSortMode = event.mode
        }
      }
    }
  }
}
