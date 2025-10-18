package app.campfire.author.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.author.api.AuthorRepository
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.screen.LibraryItemScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(AuthorDetailScreen::class, UserScope::class)
@Inject
class AuthorDetailPresenter(
  @Assisted private val screen: AuthorDetailScreen,
  @Assisted private val navigator: Navigator,
  private val authorRepository: AuthorRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val analytics: Analytics,
) : Presenter<AuthorDetailUiState> {

  @Composable
  override fun present(): AuthorDetailUiState {
    val authorContentState by remember {
      authorRepository.observeAuthor(screen.authorId)
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    val offlineDownloads by remember {
      snapshotFlow { authorContentState.dataOrNull }
        .filterNotNull()
        .flatMapLatest { author ->
          offlineDownloadManager.observeForItems(author.libraryItems)
        }
    }.collectAsState(emptyMap())

    return AuthorDetailUiState(
      authorContentState = authorContentState,
      offlineStates = offlineDownloads,
    ) { event ->
      when (event) {
        is AuthorDetailUiEvent.LibraryItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.libraryItem.id))
        }
        AuthorDetailUiEvent.Back -> navigator.pop()
      }
    }
  }
}
