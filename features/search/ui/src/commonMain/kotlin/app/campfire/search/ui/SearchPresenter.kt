package app.campfire.search.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.analytics.events.SearchEvent
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.filter.ContentFilter
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.search.api.SearchRepository
import app.campfire.search.api.SearchResult
import app.campfire.search.api.ui.SearchResultNavEvent
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias SearchPresenterFactory = (
  textFieldState: TextFieldState,
  onSearchEvent: (SearchResultNavEvent) -> Unit,
) -> SearchPresenter

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class SearchPresenter(
  private val searchRepository: SearchRepository,
  @Assisted private val textFieldState: TextFieldState,
  @Assisted private val onSearchEvent: (SearchResultNavEvent) -> Unit,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val analytics: Analytics,
) : Presenter<SearchUiState> {

  @Composable
  override fun present(): SearchUiState {
    val searchResult by remember {
      snapshotFlow { textFieldState.text }
        .flatMapLatest { query ->
          if (query.isBlank()) {
            flowOf(SearchResult.Empty)
          } else {
            searchRepository.searchCurrentLibrary(query.toString())
              // Give the user time to type
              .onStart {
                emit(SearchResult.Loading)
                delay(400.milliseconds)
                analytics.send(SearchEvent())
              }
          }
        }
    }.collectAsState(SearchResult.Empty)

    val offlineDownloads by remember {
      snapshotFlow { (searchResult as? SearchResult.Success)?.books }
        .filterNotNull()
        .flatMapLatest { items ->
          offlineDownloadManager.observeForItems(items)
        }
    }.collectAsState(emptyMap())

    return SearchUiState(
      query = textFieldState.text.toString(),
      searchResult = searchResult,
      offlineStates = offlineDownloads,
    ) { event ->
      when (event) {
        is SearchUiEvent.OnAuthorClick -> {
          analytics.send(ContentSelected(ContentType.Author))
          navigateTo(
            AuthorDetailScreen(
              event.author.id,
              event.author.name,
            ),
          )
        }

        is SearchUiEvent.OnBookClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigateTo(LibraryItemScreen(event.book.id))
        }
        is SearchUiEvent.OnGenreClick -> {
          analytics.send(ContentSelected(ContentType.Genre))
          navigateTo(LibraryScreen(ContentFilter.Genres(event.genre.name)), true)
        }
        is SearchUiEvent.OnNarratorClick -> {
          analytics.send(ContentSelected(ContentType.Narrator))
          navigateTo(
            screen = LibraryScreen(ContentFilter.Narrators(event.narrator.name)),
            resetRoot = true,
          )
        }
        is SearchUiEvent.OnSeriesClick -> {
          analytics.send(ContentSelected(ContentType.Series))
          navigateTo(
            SeriesDetailScreen(
              event.series.id,
              event.series.name,
            ),
          )
        }

        is SearchUiEvent.OnTagClick -> {
          analytics.send(ContentSelected(ContentType.Tag))
          navigateTo(LibraryScreen(ContentFilter.Tags(event.tag.name)), true)
        }
      }
    }
  }

  private fun navigateTo(screen: BaseScreen, resetRoot: Boolean = false) {
    onSearchEvent(SearchResultNavEvent(screen, resetRoot))
  }
}
