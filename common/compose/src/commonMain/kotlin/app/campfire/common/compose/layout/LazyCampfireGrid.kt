package app.campfire.common.compose.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import app.campfire.common.compose.LocalWindowSizeClass

enum class LazyContentSize {
  Small,
  Large,
}

/**
 * A custom wrapper around [LazyVerticalGrid] that takes into account the
 * [androidx.compose.material3.windowsizeclass.WindowSizeClass] and current [SupportingContentState] to
 * dynamically set the grid size for its content.
 * @see LazyVerticalGrid
 */
@Composable
fun LazyCampfireGrid(
  gridCells: (LazyContentSize) -> GridCells,
  modifier: Modifier = Modifier,
  state: LazyGridState = rememberLazyGridState(),
  contentPadding: PaddingValues = PaddingValues(),
  reverseLayout: Boolean = false,
  verticalArrangement: Arrangement.Vertical =
    if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
  horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
  content: LazyGridScope.() -> Unit,
) {
  val supportingPaneEnabled by rememberUpdatedState(LocalSupportingContentState.current)
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)

  val contentSize = if (
    supportingPaneEnabled == SupportingContentState.Closed &&
    windowSizeClass.isSupportingPaneEnabled
  ) {
    LazyContentSize.Large
  } else {
    LazyContentSize.Small
  }

  val columns = remember(contentSize) {
    gridCells(contentSize)
  }

  LazyVerticalGrid(
    columns = columns,
    state = state,
    contentPadding = contentPadding,
    verticalArrangement = verticalArrangement,
    horizontalArrangement = horizontalArrangement,
    modifier = modifier.fillMaxSize(),
    content = content,
  )
}
