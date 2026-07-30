// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.find

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
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
import app.campfire.core.di.UserScope
import app.campfire.podcasts.api.screen.FindEpisodesScreen
import app.campfire.podcasts.ui.find.composables.CenteredMessage
import app.campfire.podcasts.ui.find.composables.EpisodeFeedList
import app.campfire.podcasts.ui.find.composables.SearchInput
import app.campfire.podcasts.ui.find.composables.SelectionBottomBar
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.find_episodes_empty
import campfire.features.podcasts.ui.generated.resources.find_episodes_error
import campfire.features.podcasts.ui.generated.resources.find_episodes_forbidden
import campfire.features.podcasts.ui.generated.resources.find_episodes_no_feed_url
import campfire.features.podcasts.ui.generated.resources.find_episodes_retry
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(FindEpisodesScreen::class, UserScope::class)
@Composable
fun FindEpisodesUi(
  state: FindEpisodesUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val focusManager = LocalFocusManager.current
  val listState = rememberLazyListState()
  val isListDragging by listState.interactionSource.collectIsDraggedAsState()

  LaunchedEffect(isListDragging) {
    if (isListDragging) {
      focusManager.clearFocus()
    }
  }

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = ScaffoldDefaults.contentWindowInsets
      .exclude(WindowInsets.systemBars)
      .exclude(WindowInsets.navigationBars),
    topBar = {
      TopAppBar(
        title = {
          SearchInput(
            textFieldState = state.textFieldState,
            onBackClick = { state.eventSink(FindEpisodesUiEvent.Back) },
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
      FeedContent(
        state = state,
        listState = listState,
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 8.dp,
          bottom = 16.dp,
        ),
      )

      SelectionBottomBar(
        selectedCount = state.selectedEnclosureUrls.size,
        isQueuingDownload = state.isQueuingDownload,
        onClearClick = { state.eventSink(FindEpisodesUiEvent.ClearSelection) },
        onDownloadClick = { state.eventSink(FindEpisodesUiEvent.DownloadSelected) },
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }
}

@Composable
private fun BoxScope.FeedContent(
  state: FindEpisodesUiState,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
) {
  when (val feed = state.feedState) {
    FeedState.Loading -> CircularProgressIndicator(
      modifier = Modifier.align(Alignment.Center),
    )

    FeedState.Forbidden -> CenteredMessage(stringResource(Res.string.find_episodes_forbidden))

    FeedState.NoFeedUrl -> CenteredMessage(stringResource(Res.string.find_episodes_no_feed_url))

    FeedState.Error -> Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .align(Alignment.Center)
        .padding(horizontal = 32.dp),
    ) {
      Text(
        text = stringResource(Res.string.find_episodes_error),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(16.dp))
      Button(onClick = { state.eventSink(FindEpisodesUiEvent.Retry) }) {
        Text(stringResource(Res.string.find_episodes_retry))
      }
    }

    is FeedState.Loaded -> {
      if (feed.episodes.isEmpty()) {
        CenteredMessage(
          stringResource(
            Res.string.find_episodes_empty,
            state.textFieldState.text.toString(),
          ),
        )
      } else {
        EpisodeFeedList(
          rows = feed.episodes,
          selectedEnclosureUrls = state.selectedEnclosureUrls,
          onRowClick = { episode ->
            state.eventSink(FindEpisodesUiEvent.ToggleSelection(episode))
          },
          contentPadding = contentPadding,
          modifier = modifier,
          listState = listState,
        )
      }
    }
  }
}
