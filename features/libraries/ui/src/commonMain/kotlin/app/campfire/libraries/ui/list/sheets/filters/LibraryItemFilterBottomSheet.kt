package app.campfire.libraries.ui.list.sheets.filters

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.widgets.bottomSheetShape
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.LibraryItemFilter
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import org.jetbrains.compose.resources.stringResource

sealed interface LibraryItemFilterResult {
  data object None : LibraryItemFilterResult
  data class Selected(val filter: LibraryItemFilter?) : LibraryItemFilterResult
}

private sealed interface ItemFilter {
  data object None : ItemFilter
  data class Current(val filter: LibraryItemFilter) : ItemFilter
}

@ContributesTo(UserScope::class)
interface LibraryItemFilterBottomSheetComponent {
  val itemFilterPresenterFactory: ItemFilterPresenterFactory
}

suspend fun OverlayHost.showItemFilterOverlay(
  filter: LibraryItemFilter?,
): LibraryItemFilterResult {
  return show(
    BottomSheetOverlay<ItemFilter, LibraryItemFilterResult>(
      model = filter?.let { ItemFilter.Current(it) } ?: ItemFilter.None,
      onDismiss = { LibraryItemFilterResult.None },
      sheetShape = bottomSheetShape,
      skipPartiallyExpandedState = true,
    ) { filter, overlayNavigator ->
      LibraryItemFilterBottomSheet(
        filter = (filter as? ItemFilter.Current)?.filter,
        onSelected = {
          overlayNavigator.finish(LibraryItemFilterResult.Selected(it))
        },
      )
      Spacer(
        Modifier.navigationBarsPadding(),
      )
    },
  )
}

@Composable
private fun LibraryItemFilterBottomSheet(
  filter: LibraryItemFilter?,
  onSelected: (LibraryItemFilter?) -> Unit,
  modifier: Modifier = Modifier,
  component: LibraryItemFilterBottomSheetComponent = rememberComponent(),
) {
  val presenter = remember { component.itemFilterPresenterFactory(filter, onSelected) }
  val viewState = presenter.present()

  Column(
    modifier = modifier
      .navigationBarsPadding(),
  ) {
    var selectedFilter by remember { mutableStateOf<UiItemFilter<*>?>(null) }

    AnimatedContent(
      targetState = selectedFilter,
      transitionSpec = {
        (
          fadeIn(animationSpec = tween(220, delayMillis = 90)) +
            slideInHorizontally(animationSpec = tween(220, delayMillis = 90)) { it }
          )
          .togetherWith(slideOutHorizontally(animationSpec = tween(90)) { it })
      },
    ) { state ->
      if (state == null) {
        FilterGroupList(
          filters = viewState.filters,
          onItemClick = { selectedFilter = it },
        )
      } else {
        FilterGroupOptionList(
          selectedFilter = state,
          onBackClick = { selectedFilter = null },
          onFilterOptionClick = { value ->
            viewState.eventSink(ItemFilterUiEvent.FilterSelected(state, value))
          },
        )
      }
    }
  }
}

@Composable
private fun FilterGroupList(
  filters: List<UiItemFilter<*>>,
  onItemClick: (UiItemFilter<*>) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
  ) {
    items(filters) { filter ->
      FilterGroupListItem(
        filter = filter,
        onClick = { onItemClick(filter) },
      )
    }
  }
}

@Composable
private fun FilterGroupListItem(
  filter: UiItemFilter<*>,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DropdownMenuItem(
    text = {
      Row {
        Text(stringResource(filter.name))
        Spacer(Modifier.width(8.dp))
        Text(
          text = filter.values.size.toString(),
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier
            .background(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = MaterialTheme.shapes.small,
            )
            .padding(
              horizontal = 8.dp,
              vertical = 4.dp,
            ),
        )
      }
    },
    trailingIcon = {
      Icon(Icons.AutoMirrored.Rounded.ArrowRight, contentDescription = null)
    },
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
private fun <T : Any> FilterGroupOptionList(
  selectedFilter: UiItemFilter<T>,
  onBackClick: () -> Unit,
  onFilterOptionClick: (Any) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
  ) {
    TopAppBar(
      navigationIcon = {
        IconButton(
          onClick = onBackClick,
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
        }
      },
      title = {
        Text(text = stringResource(selectedFilter.name))
      },
      windowInsets = WindowInsets(0),
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
      ),
    )

    LazyColumn {
      items(selectedFilter.values) { option ->
        FilterOptionListItem(
          filter = selectedFilter,
          value = option,
          onClick = { onFilterOptionClick(option) },
        )
      }
    }
  }
}

@Composable
private fun <T : Any> FilterOptionListItem(
  filter: UiItemFilter<T>,
  value: T,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DropdownMenuItem(
    text = {
      Text(filter.valueLabel(value))
    },
    onClick = onClick,
    modifier = modifier,
  )
}
