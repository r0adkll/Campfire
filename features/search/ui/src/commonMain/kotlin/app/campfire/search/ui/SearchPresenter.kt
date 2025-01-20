package app.campfire.search.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.search.api.SearchRepository
import app.campfire.search.api.SearchResult
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias SearchPresenterFactory = (navigator: Navigator, requestDismiss: () -> Unit) -> SearchPresenter

@Inject
class SearchPresenter(
  private val searchRepository: SearchRepository,
  @Assisted private val navigator: Navigator,
  @Assisted private val requestDismiss: () -> Unit,
) : Presenter<SearchUiState> {

  @Composable
  override fun present(): SearchUiState {
    var query by remember { mutableStateOf("") }

    val searchResult by remember(query) {
      if (query.isBlank()) {
        flowOf(SearchResult.Empty)
      } else {
        searchRepository.searchCurrentLibrary(query)
          // Give the user time to type
          .onStart {
            emit(SearchResult.Loading)
            delay(400.milliseconds)
          }
      }
    }.collectAsState(SearchResult.Empty)

    return SearchUiState(
      query = query,
      searchResult = searchResult,
    ) { event ->
      when (event) {
        SearchUiEvent.ClearQuery -> query = ""
        SearchUiEvent.Dismiss -> requestDismiss()
        is SearchUiEvent.QueryChanged -> query = event.query

        is SearchUiEvent.OnAuthorClick -> navigateTo(
          AuthorDetailScreen(
            event.author.id,
            event.author.name,
          ),
        )

        is SearchUiEvent.OnBookClick -> navigateTo(LibraryItemScreen(event.book.id))
        is SearchUiEvent.OnGenreClick -> TODO("Navigate to LibraryItemScreen with filter information")
        is SearchUiEvent.OnNarratorClick -> TODO("Navigate to LibraryItemScreen with filter information")
        is SearchUiEvent.OnSeriesClick -> navigateTo(
          SeriesDetailScreen(
            event.series.id,
            event.series.name,
          ),
        )

        is SearchUiEvent.OnTagClick -> TODO("Navigate to LibraryItemScreen with filter information")
      }
    }
  }

  private fun navigateTo(screen: BaseScreen) {
    navigator.goTo(screen)
    requestDismiss()
  }
}
