package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

@Composable
fun ContentPagingScaffold(
  lazyPagingItems: LazyPagingItems<*>,
  emptyMessage: String,
  modifier: Modifier = Modifier,
  indicatorPadding: Dp = 0.dp,
  content: @Composable ContentPagingScaffoldScope.() -> Unit,
) {
  val scope = remember(lazyPagingItems) {
    ContentPagingScaffoldScope(lazyPagingItems)
  }

  val isEmpty = lazyPagingItems.itemCount == 0
  val isRefreshing = lazyPagingItems.loadState.refresh == LoadState.Loading

  val ptrState = rememberPullToRefreshState()
  PullToRefreshBox(
    state = ptrState,
    isRefreshing = isRefreshing,
    onRefresh = { lazyPagingItems.refresh() },
    modifier = modifier,
    indicator = {
      CampfireLoadingIndicator(
        state = ptrState,
        isRefreshing = isRefreshing && !isEmpty,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = indicatorPadding),
      )
    },
  ) {
    with(scope) {
      content()
    }

    if (isEmpty && !isRefreshing && lazyPagingItems.loadState.isIdle) {
      EmptyState(emptyMessage)
    } else if (isEmpty && isRefreshing) {
      LoadingState(
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

class ContentPagingScaffoldScope internal constructor(
  private val lazyPagingItems: LazyPagingItems<*>,
) {
  private val isAppending
    get() = lazyPagingItems.loadState.append == LoadState.Loading

  fun LazyGridScope.appendingIndicatorItem() {
    if (isAppending) {
      item(
        span = { GridItemSpan(maxLineSpan) },
      ) {
        AppendLoadingIndicator()
      }
    }
  }

  fun LazyListScope.appendingIndicatorItem() {
    if (isAppending) {
      item {
        AppendLoadingIndicator()
      }
    }
  }

  @Composable
  fun PlaceholderItem(
    modifier: Modifier = Modifier,
  ) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator()
    }
  }
}

@Composable
private fun AppendLoadingIndicator(
  modifier: Modifier = Modifier,
) {
  CircularProgressIndicator(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
      .wrapContentWidth(Alignment.CenterHorizontally),
  )
}
