package app.campfire.author.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.paging.cachedIn
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.author.api.AuthorPager
import app.campfire.author.api.AuthorRepository
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.AuthorsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.map
import app.campfire.core.di.UserScope
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

internal const val INVALID_AUTHOR_COUNT = -1

@CircuitInject(AuthorsScreen::class, UserScope::class)
@Inject
class AuthorsPresenter(
  @Assisted private val navigator: Navigator,
  private val authorRepository: AuthorRepository,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<AuthorsUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): AuthorsUiState {
    val scope = rememberCoroutineScope()

    val sortMode by remember {
      settings.observeAuthorsSortMode()
    }.collectAsState(settings.authorsSortMode)

    val sortDirection by remember {
      settings.observeAuthorsSortDirection()
    }.collectAsState(settings.authorsSortDirection)

    val authorPagerState by remember(sortMode, sortDirection) {
      authorRepository.observeAuthorsPager(
        sortMode = sortMode,
        sortDirection = sortDirection,
      )
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out AuthorPager>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val authorCount by remember {
      snapshotFlow { authorPagerState.dataOrNull }
        .flatMapLatest { pager ->
          pager?.countFlow
            ?.map { count -> count ?: INVALID_AUTHOR_COUNT }
            ?: flowOf(INVALID_AUTHOR_COUNT)
        }
    }.collectAsState(INVALID_AUTHOR_COUNT)

    val authorPagingContentState by remember {
      derivedStateOf {
        authorPagerState.map { it.pager.flow.cachedIn(scope) }
      }
    }

    return AuthorsUiState(
      authorContentState = authorPagingContentState,
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
