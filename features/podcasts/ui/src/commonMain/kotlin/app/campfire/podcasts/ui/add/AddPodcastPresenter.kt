// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.add

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.core.di.UserScope
import app.campfire.core.model.User
import app.campfire.libraries.api.LibraryRepository
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.screen.AddPodcastBuilderScreen
import app.campfire.podcasts.api.screen.AddPodcastScreen
import app.campfire.podcasts.api.toDraft
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

private const val DEBOUNCE_MS = 300L
private val URL_REGEX = Regex("^https?://\\S+", RegexOption.IGNORE_CASE)

@CircuitInject(AddPodcastScreen::class, UserScope::class)
@Inject
class AddPodcastPresenter(
  @Assisted private val screen: AddPodcastScreen,
  @Assisted private val navigator: Navigator,
  private val analytics: Analytics,
  private val podcastsRepository: PodcastsRepository,
  private val libraryRepository: LibraryRepository,
  private val userRepository: UserRepository,
) : NonPausablePresenter<AddPodcastUiState> {

  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  @Composable
  override fun present(): AddPodcastUiState {
    val scope = rememberRetainedCoroutineScope()
    val textFieldState = rememberRetained { TextFieldState() }
    var retryToken by rememberRetained { mutableStateOf(0) }
    var searchRegion by rememberRetained { mutableStateOf<String?>(null) }

    val currentUser by userRepository.userFlow.collectAsState()
    val canAddPodcasts = currentUser.type == User.Type.Admin || currentUser.type == User.Type.Root

    LaunchedEffect(screen.libraryId) {
      libraryRepository.getAddPodcastContext(screen.libraryId)
        .onSuccess { searchRegion = it.searchRegion?.takeIf { region -> region.isNotBlank() } }
    }

    // [searchRegion] is intentionally NOT a key here: the LaunchedEffect above resolves it
    // asynchronously from null to its real value, and re-keying on it would replace the shared
    // flow without cancelling the old shareIn launch (it lives in the retained scope), leaking a
    // second collector that also fires the search on the next keystroke. Instead the closure
    // reads [searchRegion] via its Compose-state delegate, which always returns the latest value
    // when [resolveSearch] runs.
    val searchState: SearchState by rememberRetained(retryToken) {
      snapshotFlow {
        textFieldState.text.toString().trim()
      }.flatMapLatest<String, SearchState> { query ->
        delay(DEBOUNCE_MS)
        when {
          query.isEmpty() -> flowOf(SearchState.Idle)
          URL_REGEX.matches(query) -> flow {
            emit(SearchState.Loading)
            emit(resolveFeedPreview(query, canAddPodcasts))
          }

          else -> flow {
            emit(SearchState.Loading)
            emit(resolveSearch(query, searchRegion))
          }
        }
      }.shareIn(
        scope = scope,
        started = SharingStarted.Lazily,
        // replay latest multicasted paging data since it is re-connectable.
        replay = 1,
      )
    }.collectAsState(SearchState.Idle)

    return AddPodcastUiState(
      textFieldState = textFieldState,
      searchState = searchState,
      canAddPodcasts = canAddPodcasts,
      eventSink = { event ->
        when (event) {
          AddPodcastUiEvent.Back -> navigator.pop()
          AddPodcastUiEvent.Retry -> retryToken++
          is AddPodcastUiEvent.ResultTapped -> {
            // Navigate immediately with iTunes-only data. The builder fetches the feed in the
            // background and hydrates fields (description, episode order) once it lands.
            analytics.send(ActionEvent("add_podcast_select_result", Click))
            navigator.goTo(AddPodcastBuilderScreen(screen.libraryId, event.result.toDraft()))
          }

          is AddPodcastUiEvent.PreviewTapped -> {
            analytics.send(ActionEvent("add_podcast_select_feed", Click))
            navigator.goTo(AddPodcastBuilderScreen(screen.libraryId, event.draft))
          }
        }
      },
    )
  }

  private suspend fun resolveFeedPreview(url: String, canAdd: Boolean): SearchState {
    if (!canAdd) return SearchState.Forbidden
    return podcastsRepository.fetchPodcastFeedDetails(url).fold(
      onSuccess = { SearchState.FeedPreview(it.draft) },
      onFailure = { SearchState.Error },
    )
  }

  private suspend fun resolveSearch(query: String, country: String?): SearchState {
    return podcastsRepository.searchPodcasts(query, country).fold(
      onSuccess = { hits ->
        if (hits.isEmpty()) SearchState.NoResults(query) else SearchState.Results(hits)
      },
      onFailure = { SearchState.Error },
    )
  }
}
