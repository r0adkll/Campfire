package app.campfire.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.randomEmptyMessage
import app.campfire.common.screens.HomeScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Series
import app.campfire.core.model.ShelfEntity
import app.campfire.core.offline.OfflineStatus
import app.campfire.home.api.FeedResponse
import app.campfire.home.ui.composables.ShelfListItem
import app.campfire.ui.appbar.CampfireAppBar
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import campfire.features.home.ui.generated.resources.Res
import campfire.features.home.ui.generated.resources.home_feed_load_error
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(HomeScreen::class, UserScope::class)
@Composable
fun HomeScreen(
  state: HomeUiState,
  campfireAppbar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val appBarBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  AttachScrollBehaviorToLocalNavigationBar(appBarBehavior)

  Scaffold(
    topBar = {
      // Injected appbar that injects its own presenter to consistently load its state
      // across multiple services.
      campfireAppbar(
        Modifier,
        appBarBehavior,
      )
    },
    modifier = modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (val feed = state.homeFeed) {
      FeedResponse.Loading -> LoadingListState(Modifier.padding(paddingValues))
      is FeedResponse.Error -> {
        val reason = when (feed) {
          is FeedResponse.Error.Exception ->
            feed.error.message
              ?: feed.error::class.simpleName
              ?: "<Unknown error>"

          is FeedResponse.Error.Message -> feed.message
        }
        ErrorListState(
          stringResource(Res.string.home_feed_load_error, reason),
          modifier = Modifier.padding(paddingValues),
        )
      }

      is FeedResponse.Success -> if (state.homeFeed.data.isEmpty()) {
        EmptyState(randomEmptyMessage())
      } else {
        LoadedState(
          shelves = state.homeFeed.data,
          offlineStatus = { libraryItemId ->
            state.offlineStates[libraryItemId].asWidgetStatus()
          },
          progressStatus = { libraryItemId ->
            state.progressStates[libraryItemId]
          },
          contentPadding = paddingValues,
          onItemClick = { shelf, item ->
            when (item) {
              is LibraryItem -> state.eventSink(
                HomeUiEvent.OpenLibraryItem(item, item.id + shelf.id),
              )

              is Author -> state.eventSink(HomeUiEvent.OpenAuthor(item))
              is Series -> state.eventSink(HomeUiEvent.OpenSeries(item))
              else -> Unit
            }
          },
        )
      }
    }
  }
}

@Composable
private fun LoadedState(
  shelves: List<UiShelf<ShelfEntity>>,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  progressStatus: (LibraryItemId) -> MediaProgress?,
  onItemClick: (UiShelf<*>, Any) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyListState = rememberLazyListState(),
) {
  LazyColumn(
    state = state,
    modifier = modifier,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(shelves) { shelf ->
      ShelfListItem(
        shelf = shelf,
        onItemClick = { onItemClick(shelf, it) },
        offlineStatus = offlineStatus,
        progressStatus = progressStatus,
      )
    }
  }
}
