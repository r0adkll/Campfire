package app.campfire.author.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.layout.LazyCampfireGrid
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.widgets.AuthorCard
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.AuthorsScreen
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Author
import app.campfire.ui.appbar.CampfireAppBar
import campfire.features.author.ui.generated.resources.Res
import campfire.features.author.ui.generated.resources.error_authors_items_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(AuthorsScreen::class, UserScope::class)
@Composable
fun Authors(
  state: AuthorsUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)
  val appBarBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    topBar = {
      if (!windowSizeClass.isSupportingPaneEnabled) {
        // Injected appbar that injects its own presenter to consistently load its state
        // across multiple services.
        campfireAppBar(
          { },
          Modifier,
          appBarBehavior,
        )
      }
    },
    modifier = modifier.fluentIf(!windowSizeClass.isSupportingPaneEnabled) {
      nestedScroll(appBarBehavior.nestedScrollConnection)
    },
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (state.authorContentState) {
      AuthorsContentState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      AuthorsContentState.Error -> ErrorListState(
        message = stringResource(Res.string.error_authors_items_message),
        modifier = Modifier.padding(paddingValues),
      )

      is AuthorsContentState.Loaded -> LoadedState(
        items = state.authorContentState.authors,
        onAuthorClick = { state.eventSink(AuthorsUiEvent.AuthorClick(it)) },
        contentPadding = paddingValues,
      )
    }
  }
}

@Composable
private fun LoadedState(
  items: List<Author>,
  onAuthorClick: (Author) -> Unit,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
  state: LazyGridState = rememberLazyGridState(),
) {
  LazyCampfireGrid(
    state = state,
    modifier = modifier,
    contentPadding = contentPadding + PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(
      items = items,
      key = { it.id },
    ) { author ->
      AuthorCard(
        author = author,
        modifier = Modifier.clickable {
          onAuthorClick(author)
        },
      )
    }
  }
}
