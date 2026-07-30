// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.layout.DefaultAdaptiveColumnSize
import app.campfire.common.compose.layout.LargeAdaptiveColumnSize
import app.campfire.common.compose.layout.LazyCampfireGrid
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
import app.campfire.ui.appbar.CampfireAppBar
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import app.campfire.ui.navigation.bar.CampfireNavigationBarWindowInsets
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.empty_playlists_message
import campfire.features.playlists.ui.generated.resources.error_playlists_message
import campfire.features.playlists.ui.generated.resources.filter_bar_playlist_count
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
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
        icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
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
      .exclude(WindowInsets.navigationBars)
      .add(CampfireNavigationBarWindowInsets),
  ) { paddingValues ->
    when (state.playlistContentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_playlists_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedState(
        items = state.playlistContentState.data,
        displayState = state.displayState,
        onPlaylistClick = { state.eventSink(PlaylistsUiEvent.PlaylistClick(it)) },
        onToggleDisplayState = { state.eventSink(PlaylistsUiEvent.ToggleDisplayState) },
        contentPadding = paddingValues,
        state = gridState,
      )
    }
  }
}

@Composable
private fun LoadedState(
  items: List<Playlist>,
  displayState: GroupDisplayState,
  onPlaylistClick: (Playlist) -> Unit,
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

    if (items.isEmpty()) {
      EmptyState(
        message = stringResource(Res.string.empty_playlists_message),
      )
    }
  }
}

private val ContentPadding = 16.dp
private val FabBottomOffset = 88.dp
