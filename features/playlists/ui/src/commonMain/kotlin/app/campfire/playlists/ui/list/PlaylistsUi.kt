// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.OverlappedNavigationBarInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.PlaylistAdd
import app.campfire.common.compose.layout.DefaultAdaptiveColumnSize
import app.campfire.common.compose.layout.LargeAdaptiveColumnSize
import app.campfire.common.compose.layout.LazyCampfireGrid
import app.campfire.common.compose.widgets.CampfireLoadingIndicator
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.FilterBar
import app.campfire.common.compose.widgets.ItemCollectionCard
import app.campfire.common.compose.widgets.ItemCollectionGridCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Playlist
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import app.campfire.playlists.api.screen.PlaylistsScreen
import app.campfire.playlists.ui.sheets.EditPlaylistResult
import app.campfire.playlists.ui.sheets.showEditPlaylistBottomSheet
import app.campfire.sessions.api.QueuedEntry
import app.campfire.ui.appbar.CampfireAppBar
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import app.campfire.ui.navigation.bar.CampfireNavigationBarWindowInsets
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.empty_playlists_message
import campfire.features.playlists.ui.generated.resources.error_playlists_message
import campfire.features.playlists.ui.generated.resources.filter_bar_playlist_count
import campfire.features.playlists.ui.generated.resources.playlists_queue_card_count
import campfire.features.playlists.ui.generated.resources.playlists_queue_card_title
import campfire.features.playlists.ui.generated.resources.playlists_queue_card_up_next
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(PlaylistsScreen::class, UserScope::class)
@Composable
fun Playlists(
  state: PlaylistsUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()

  val appBarBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  AttachScrollBehaviorToLocalNavigationBar(appBarBehavior)

  val overlayHost = LocalOverlayHost.current
  val gridState = rememberLazyGridState()
  Scaffold(
    topBar = {
      // Injected appbar that injects its own presenter to consistently load its state
      // across multiple services.
      campfireAppBar(
        Modifier,
        appBarBehavior,
      )
    },
    floatingActionButton = {
      val expanded by remember {
        derivedStateOf {
          gridState.firstVisibleItemIndex == 0 &&
            gridState.firstVisibleItemScrollOffset < 50
        }
      }
      SmallExtendedFloatingActionButton(
        expanded = expanded,
        text = { Text("New playlist") },
        icon = { Icon(CampfireIcons.Rounded.PlaylistAdd, contentDescription = null) },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        onClick = {
          scope.launch {
            val result = overlayHost.showEditPlaylistBottomSheet()
            if (result is EditPlaylistResult.Success) {
              // Something here?
            }
          }
        },
      )
    },
    modifier = modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets
      .exclude(OverlappedNavigationBarInsets)
      .add(CampfireNavigationBarWindowInsets),
  ) { paddingValues ->
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
      isRefreshing = state.isRefreshing,
      onRefresh = { state.eventSink(PlaylistsUiEvent.Refresh) },
      state = pullToRefreshState,
      modifier = Modifier.fillMaxSize(),
      indicator = {
        CampfireLoadingIndicator(
          state = pullToRefreshState,
          isRefreshing = state.isRefreshing,
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = paddingValues.calculateTopPadding()),
        )
      },
    ) {
      when (state.playlistContentState) {
        LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
        LoadState.Error -> ErrorListState(
          message = stringResource(Res.string.error_playlists_message),
          modifier = Modifier.padding(paddingValues),
        )

        is LoadState.Loaded -> LoadedState(
          items = state.playlistContentState.data,
          queue = state.queue,
          displayState = state.displayState,
          onPlaylistClick = { state.eventSink(PlaylistsUiEvent.PlaylistClick(it)) },
          onQueueClick = { state.eventSink(PlaylistsUiEvent.QueueClick) },
          onToggleDisplayState = { state.eventSink(PlaylistsUiEvent.ToggleDisplayState) },
          contentPadding = paddingValues,
          state = gridState,
        )
      }
    }
  }
}

@Composable
private fun LoadedState(
  items: List<Playlist>,
  queue: List<QueuedEntry>,
  displayState: GroupDisplayState,
  onPlaylistClick: (Playlist) -> Unit,
  onQueueClick: () -> Unit,
  onToggleDisplayState: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyGridState = rememberLazyGridState(),
) {
  val adaptiveColumnSize = when (displayState) {
    GroupDisplayState.List -> LargeAdaptiveColumnSize
    GroupDisplayState.Grid -> DefaultAdaptiveColumnSize
  }

  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    LazyCampfireGrid(
      columns = GridCells.Adaptive(adaptiveColumnSize),
      state = state,
      contentPadding = contentPadding +
        PaddingValues(horizontal = ContentPadding) +
        PaddingValues(bottom = FabBottomOffset),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      item(
        span = { GridItemSpan(maxLineSpan) },
        key = "filter-bar",
      ) {
        FilterBar(
          count = {
            Text(pluralStringResource(Res.plurals.filter_bar_playlist_count, items.size, items.size))
          },
          itemDisplayState = when (displayState) {
            GroupDisplayState.List -> ItemDisplayState.List
            GroupDisplayState.Grid -> ItemDisplayState.Grid
          },
          onDisplayStateClick = onToggleDisplayState,
        )
      }

      if (queue.isNotEmpty()) {
        item(key = "playback-queue") {
          QueueCard(
            queue = queue,
            displayState = displayState,
            onClick = onQueueClick,
            modifier = Modifier
              .fillMaxWidth()
              .animateItem(),
          )
        }
      }

      if (items.isEmpty() && queue.isNotEmpty()) {
        // The full-screen empty state would draw over the queue card, so say there are no
        // playlists below it instead.
        item(
          span = { GridItemSpan(maxLineSpan) },
          key = "empty-playlists",
        ) {
          Text(
            text = stringResource(Res.string.empty_playlists_message),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = EmptyInlinePaddingHorizontal, vertical = EmptyInlinePaddingVertical)
              .animateItem(),
          )
        }
      }

      items(
        count = items.size,
        key = { items[it].id },
      ) { index ->
        val playlist = items[index]
        when (displayState) {
          GroupDisplayState.List -> ItemCollectionCard(
            sharedTransitionKey = playlist.id,
            name = playlist.name,
            description = playlist.description,
            items = playlist.items.map { it.libraryItem },
            onClick = { onPlaylistClick(playlist) },
            modifier = Modifier
              .fillMaxWidth()
              .animateItem(),
          )
          GroupDisplayState.Grid -> ItemCollectionGridCard(
            sharedTransitionKey = playlist.id,
            name = playlist.name,
            items = playlist.items.map { it.libraryItem },
            onClick = { onPlaylistClick(playlist) },
            modifier = Modifier
              .fillMaxWidth()
              .animateItem(),
          )
        }
      }
    }

    if (items.isEmpty() && queue.isEmpty()) {
      EmptyState(
        message = stringResource(Res.string.empty_playlists_message),
      )
    }
  }
}

/**
 * The playback queue, styled like the playlists around it so it reads as one of them, in the
 * secondary container color so it stands apart.
 */
@Composable
private fun QueueCard(
  queue: List<QueuedEntry>,
  displayState: GroupDisplayState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val name = stringResource(Res.string.playlists_queue_card_title)
  val items = queue.map { it.libraryItem }
  val colors = CardDefaults.elevatedCardColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
  )
  when (displayState) {
    GroupDisplayState.List -> ItemCollectionCard(
      sharedTransitionKey = QueueSharedTransitionKey,
      name = name,
      description = stringResource(Res.string.playlists_queue_card_up_next, queue.first().title),
      items = items,
      onClick = onClick,
      colors = colors,
      modifier = modifier,
    )
    GroupDisplayState.Grid -> ItemCollectionGridCard(
      sharedTransitionKey = QueueSharedTransitionKey,
      name = name,
      subtitle = pluralStringResource(Res.plurals.playlists_queue_card_count, queue.size, queue.size),
      items = items,
      onClick = onClick,
      colors = colors,
      modifier = modifier,
    )
  }
}

/** The episode title for podcast entries, as the queue itself shows them. */
private val QueuedEntry.title: String
  get() = episode?.title ?: libraryItem.media.metadata.title.orEmpty()

private const val QueueSharedTransitionKey = "playback-queue"
private val ContentPadding = 16.dp
private val FabBottomOffset = 88.dp
private val EmptyInlinePaddingHorizontal = 24.dp
private val EmptyInlinePaddingVertical = 32.dp
