package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.map
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUser
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.ui.detail.book.BookPresenter
import app.campfire.libraries.ui.detail.podcast.PodcastPresenter
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.SwatchSelector
import app.campfire.ui.theming.api.ThemeManager
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.r0adkll.swatchbuckler.compose.Schema
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Inject
class LibraryItemPresenter(
  @Assisted private val screen: LibraryItemScreen,
  @Assisted private val navigator: Navigator,
  private val userSession: UserSession,
  private val repository: LibraryItemRepository,
  private val bookPresenter: BookPresenter,
  private val podcastPresenter: PodcastPresenter,
  private val themeManager: ThemeManager,
  private val themeSettings: ThemeSettings,
  private val dispatcherProvider: DispatcherProvider,
  private val offlineDownloadManager: OfflineDownloadManager,
) : NonPausablePresenter<LibraryItemUiState> {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): LibraryItemUiState {
    val scope = rememberCoroutineScope()

    val libraryItemContentState by remember {
      repository.observeLibraryItem(screen.libraryItemId)
        .map { LoadState.Loaded(it) as LoadState<LibraryItem> }
        .catch { emit(LoadState.Error as LoadState<LibraryItem>) }
    }.collectAsState(LoadState.Loading)

    val contentState = libraryItemContentState.map { libraryItem ->
      when (libraryItem.mediaType) {
        MediaType.Book -> bookPresenter.present(screen, navigator, libraryItem)
        MediaType.Podcast -> podcastPresenter.present(screen, navigator, libraryItem)
      }
    }

    // Theming

    val isDynamicThemingEnabled by remember {
      themeSettings.observeDynamicallyThemeItemDetail()
    }.collectAsState()

    val theme by remember(isDynamicThemingEnabled) {
      if (!isDynamicThemingEnabled) {
        flowOf(null)
      } else themeManager.observeThemeFor(
        key = screen.libraryItemId,
        colorSelector = SwatchSelector.Dominant,
        schema = Schema.Expressive,
      )
    }.collectAsState(null)

    val swatch by remember(isDynamicThemingEnabled) {
      if (!isDynamicThemingEnabled) {
        flowOf(null)
      } else themeManager.observeSwatchFor(screen.libraryItemId)
    }.collectAsState(null)

    var errorMessage by rememberRetained { mutableStateOf<String?>(null) }

    return LibraryItemUiState(
      user = userSession.requiredUser,
      libraryItem = libraryItemContentState.dataOrNull,
      theme = theme,
      swatch = swatch,
      contentState = contentState,
      errorMessage = errorMessage,
    ) { event ->
      when (event) {
        LibraryItemUiEvent.OnBack -> navigator.pop()

        is LibraryItemUiEvent.SeedColorChange -> {
          scope.launch(dispatcherProvider.computation) {
            themeManager.enqueue(
              key = screen.libraryItemId,
              seedColor = event.seedColor,
            )
          }
        }

        is LibraryItemUiEvent.DeleteItemClick -> {
          val item = libraryItemContentState.dataOrNull ?: return@LibraryItemUiState
          scope.launch {
            repository.deleteLibraryItem(screen.libraryItemId, event.hardDelete)
              .onSuccess {
                runCatching {
                  offlineDownloadManager.delete(item)
                  (item.media as? Media.Podcast)?.episodes?.forEach { episode ->
                    offlineDownloadManager.deleteEpisode(item, episode)
                  }
                }
                  .onFailure { bark(throwable = it) { "Failed to clean up offline downloads after delete" } }
                navigator.pop()
              }
              .onFailure { error ->
                bark(throwable = error) { "Failed to delete library item ${screen.libraryItemId}" }
                errorMessage = error.message ?: "Couldn't delete podcast"
              }
          }
        }

        LibraryItemUiEvent.ClearError -> {
          errorMessage = null
        }

        // If we don't handle the event then pass it through the content state
        // created by one of the delegating presenters
        else -> contentState.dataOrNull?.eventSink(event)
      }
    }
  }
}
