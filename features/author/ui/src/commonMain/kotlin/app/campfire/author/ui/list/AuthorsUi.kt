package app.campfire.author.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.layout.LazyCampfireGrid
import app.campfire.common.compose.widgets.AuthorCard
import app.campfire.common.compose.widgets.ContentPagingScaffold
import app.campfire.common.compose.widgets.FilterBar
import app.campfire.common.screens.AuthorsScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.core.settings.AuthorSortModes
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.filters.SortModeUi
import app.campfire.ui.appbar.CampfireAppBar
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import campfire.features.author.ui.generated.resources.Res
import campfire.features.author.ui.generated.resources.empty_authors_message
import campfire.features.author.ui.generated.resources.filter_bar_author_count
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@CircuitInject(AuthorsScreen::class, UserScope::class)
@Composable
fun Authors(
  state: AuthorsUiState,
  campfireAppBar: CampfireAppBar,
  sortModeUi: SortModeUi,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val appBarBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  AttachScrollBehaviorToLocalNavigationBar(appBarBehavior)

  Scaffold(
    topBar = {
      // Injected appbar that injects its own presenter to consistently load its state
      // across multiple services.
      campfireAppBar(
        Modifier,
        appBarBehavior,
      )
    },
    modifier = modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    val overlayHost = LocalOverlayHost.current
    LoadedState(
      lazyPagingItems = state.lazyPagingItems,
      numAuthors = state.numAuthors,
      sortMode = state.sortMode,
      sortDirection = state.sortDirection,
      onAuthorClick = { state.eventSink(AuthorsUiEvent.AuthorClick(it)) },
      onSortClick = {
        scope.launch {
          val updatedSortMode = sortModeUi.showContentSortModeBottomSheet(
            overlayHost,
            state.sortMode,
            state.sortDirection,
            AuthorSortModes,
          )
          if (updatedSortMode != null) {
            state.eventSink(AuthorsUiEvent.SortModeSelected(updatedSortMode))
          }
        }
      },
      contentPadding = paddingValues,
    )
  }
}

@Composable
private fun LoadedState(
  lazyPagingItems: LazyPagingItems<Author>,
  numAuthors: Int,
  sortMode: ContentSortMode,
  sortDirection: SortDirection,
  onAuthorClick: (Author) -> Unit,
  onSortClick: () -> Unit,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
  state: LazyGridState = rememberLazyGridState(),
) {
  ContentPagingScaffold(
    modifier = modifier,
    lazyPagingItems = lazyPagingItems,
    emptyMessage = stringResource(Res.string.empty_authors_message),
    indicatorPadding = contentPadding.calculateTopPadding(),
  ) {
    LazyCampfireGrid(
      state = state,
      contentPadding = contentPadding + PaddingValues(
        horizontal = 16.dp,
      ),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item(
        span = { GridItemSpan(this.maxLineSpan) },
        key = "filter-bar",
      ) {
        FilterBar(
          count = {
            if (numAuthors != INVALID_AUTHOR_COUNT) {
              Text(pluralStringResource(Res.plurals.filter_bar_author_count, numAuthors, numAuthors))
            } else {
              Text("--")
            }
          },
          itemDisplayState = ItemDisplayState.Grid,
          sortMode = sortMode,
          sortDirection = sortDirection,
          onSortClick = onSortClick,
        )
      }

      items(
        count = lazyPagingItems.itemCount,
        key = lazyPagingItems.itemKey { it.id },
        contentType = lazyPagingItems.itemContentType { "author-card" },
      ) { index ->
        val author = lazyPagingItems[index]
        if (author == null) {
          PlaceholderItem()
        } else {
          AuthorCard(
            author = author,
            onClick = { onAuthorClick(author) },
          )
        }
      }

      appendingIndicatorItem()
    }
  }
}
