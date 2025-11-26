package app.campfire.search.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.core.di.UserScope
import app.campfire.search.api.ui.LocalSearchEventHandler
import app.campfire.search.api.ui.SearchComponent
import app.campfire.search.ui.composables.SearchResultContent
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class CampfireSearchComponent(
  private val searchPresenterFactory: SearchPresenterFactory,
) : SearchComponent {

  @Composable
  override fun ResultContent(textFieldState: TextFieldState) {
    val localSearchEventHandler by rememberUpdatedState(LocalSearchEventHandler.current)

    val presenter = remember {
      searchPresenterFactory(textFieldState, localSearchEventHandler)
    }

    val uiState = presenter.present()
    SearchResultContent(uiState)
  }

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Composable
  private fun SearchResultContent(
    state: SearchUiState,
  ) = SharedElementTransitionLayout {
    val eventSink = state.eventSink

    SearchResultContent(
      modifier = Modifier.imePadding(),
      query = state.query,
      results = state.searchResult,
      offlineStatus = { state.offlineStates[it].asWidgetStatus() },
      onBookClick = { book -> eventSink(SearchUiEvent.OnBookClick(book)) },
      onNarratorClick = { narrator -> eventSink(SearchUiEvent.OnNarratorClick(narrator)) },
      onAuthorClick = { author -> eventSink(SearchUiEvent.OnAuthorClick(author)) },
      onSeriesClick = { series -> eventSink(SearchUiEvent.OnSeriesClick(series)) },
      onTagClick = { tag -> eventSink(SearchUiEvent.OnTagClick(tag)) },
      onGenreClick = { genre -> eventSink(SearchUiEvent.OnGenreClick(genre)) },
    )
  }
}
