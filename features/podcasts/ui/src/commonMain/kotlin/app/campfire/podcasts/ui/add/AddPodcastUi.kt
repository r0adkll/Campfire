// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.add

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.core.di.UserScope
import app.campfire.podcasts.api.screen.AddPodcastScreen
import app.campfire.podcasts.ui.add.composables.AddPodcastCenteredMessage
import app.campfire.podcasts.ui.add.composables.AddPodcastErrorState
import app.campfire.podcasts.ui.add.composables.AddPodcastSearchInput
import app.campfire.podcasts.ui.add.composables.PodcastFeedPreviewCard
import app.campfire.podcasts.ui.add.composables.PodcastSearchResultRow
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_empty
import campfire.features.podcasts.ui.generated.resources.add_podcast_forbidden
import campfire.features.podcasts.ui.generated.resources.add_podcast_idle
import campfire.features.podcasts.ui.generated.resources.add_podcast_retry
import campfire.features.podcasts.ui.generated.resources.add_podcast_search_error
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(AddPodcastScreen::class, UserScope::class)
@Composable
fun AddPodcastUi(
  state: AddPodcastUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val focusManager = LocalFocusManager.current
  val listState = rememberLazyListState()
  val isListDragging by listState.interactionSource.collectIsDraggedAsState()

  LaunchedEffect(isListDragging) {
    if (isListDragging) focusManager.clearFocus()
  }

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets
      .exclude(WindowInsets.systemBars)
      .exclude(WindowInsets.navigationBars),
    topBar = {
      TopAppBar(
        title = {
          AddPodcastSearchInput(
            textFieldState = state.textFieldState,
            onBackClick = { state.eventSink(AddPodcastUiEvent.Back) },
            modifier = Modifier
              .fillMaxWidth()
              .padding(end = 8.dp),
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
        scrollBehavior = scrollBehavior,
      )
    },
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
    ) {
      if (!state.canAddPodcasts) {
        AddPodcastCenteredMessage(stringResource(Res.string.add_podcast_forbidden))
      } else {
        SearchContent(
          state = state,
          listState = listState,
          contentPadding = PaddingValues(16.dp),
        )
      }
    }
  }
}

@Composable
private fun BoxScope.SearchContent(
  state: AddPodcastUiState,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
) {
  when (val search = state.searchState) {
    SearchState.Idle -> AddPodcastCenteredMessage(stringResource(Res.string.add_podcast_idle))

    SearchState.Loading -> CircularProgressIndicator(
      modifier = Modifier.align(Alignment.Center),
    )

    SearchState.Forbidden -> AddPodcastCenteredMessage(
      stringResource(Res.string.add_podcast_forbidden),
    )

    SearchState.Error -> AddPodcastErrorState(
      text = stringResource(Res.string.add_podcast_search_error),
      retryLabel = stringResource(Res.string.add_podcast_retry),
      onRetry = { state.eventSink(AddPodcastUiEvent.Retry) },
    )

    is SearchState.NoResults -> AddPodcastCenteredMessage(
      stringResource(Res.string.add_podcast_empty, search.query),
    )

    is SearchState.Results -> LazyColumn(
      state = listState,
      contentPadding = contentPadding,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = modifier.fillMaxSize(),
    ) {
      items(
        items = search.hits,
        key = { it.itunesId },
      ) { hit ->
        PodcastSearchResultRow(
          result = hit,
          onClick = { state.eventSink(AddPodcastUiEvent.ResultTapped(hit)) },
        )
      }
    }

    is SearchState.FeedPreview -> LazyColumn(
      state = listState,
      contentPadding = contentPadding,
      modifier = modifier.fillMaxSize(),
    ) {
      item {
        PodcastFeedPreviewCard(
          draft = search.draft,
          onContinue = { state.eventSink(AddPodcastUiEvent.PreviewTapped(search.draft)) },
        )
      }
    }
  }
}
