package app.campfire.libraries.ui.list.sheets.filters

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.widgets.bottomSheetShape
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.libraries.api.LibraryItemFilter
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.filter_category_author
import campfire.features.libraries.ui.generated.resources.filter_category_genre
import campfire.features.libraries.ui.generated.resources.filter_category_language
import campfire.features.libraries.ui.generated.resources.filter_category_missing
import campfire.features.libraries.ui.generated.resources.filter_category_narrator
import campfire.features.libraries.ui.generated.resources.filter_category_progress
import campfire.features.libraries.ui.generated.resources.filter_category_series
import campfire.features.libraries.ui.generated.resources.filter_category_tag
import campfire.features.libraries.ui.generated.resources.filter_category_tracks
import campfire.features.libraries.ui.generated.resources.filter_value_missing_asin
import campfire.features.libraries.ui.generated.resources.filter_value_missing_authors
import campfire.features.libraries.ui.generated.resources.filter_value_missing_description
import campfire.features.libraries.ui.generated.resources.filter_value_missing_genres
import campfire.features.libraries.ui.generated.resources.filter_value_missing_isbn
import campfire.features.libraries.ui.generated.resources.filter_value_missing_language
import campfire.features.libraries.ui.generated.resources.filter_value_missing_narrators
import campfire.features.libraries.ui.generated.resources.filter_value_missing_publishedYear
import campfire.features.libraries.ui.generated.resources.filter_value_missing_publisher
import campfire.features.libraries.ui.generated.resources.filter_value_missing_series
import campfire.features.libraries.ui.generated.resources.filter_value_missing_subtitle
import campfire.features.libraries.ui.generated.resources.filter_value_missing_tags
import campfire.features.libraries.ui.generated.resources.filter_value_progress_finished
import campfire.features.libraries.ui.generated.resources.filter_value_progress_in_progress
import campfire.features.libraries.ui.generated.resources.filter_value_progress_not_finished
import campfire.features.libraries.ui.generated.resources.filter_value_progress_not_started
import campfire.features.libraries.ui.generated.resources.filter_value_tracks_multi
import campfire.features.libraries.ui.generated.resources.filter_value_tracks_single
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
      Impression {
        ScreenViewEvent("LibraryItemFilter", ScreenType.Overlay)
      }

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
        if (targetState == null) {
          (fadeIn() + slideInHorizontally { -it })
            .togetherWith(slideOutHorizontally { it })
        } else {
          slideInHorizontally { it }
            .togetherWith(fadeOut() + slideOutHorizontally { -it })
        }
      },
    ) { state ->
      if (state == null) {
        FilterGroupList(
          filters = viewState.filters,
          currentItemFilter = viewState.selected,
          onItemClick = { selectedFilter = it },
          onClearClick = {
            viewState.eventSink(ItemFilterUiEvent.ClearFilter)
          },
        )
      } else {
        FilterGroupOptionList(
          selectedFilter = state,
          currentItemFilter = viewState.selected,
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
  currentItemFilter: LibraryItemFilter?,
  onItemClick: (UiItemFilter<*>) -> Unit,
  onClearClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    if (currentItemFilter != null) {
      CurrentFilterListItem(
        currentItemFilter = currentItemFilter,
        onClearClick = onClearClick,
      )
    }
    LazyColumn {
      items(filters) { filter ->
        FilterGroupListItem(
          filter = filter,
          onClick = { onItemClick(filter) },
        )
      }
    }
  }
}

@Composable
private fun CurrentFilterListItem(
  currentItemFilter: LibraryItemFilter,
  onClearClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    modifier = modifier,
    headlineContent = {
      Text(
        buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(filterGroupLabel(currentItemFilter))
            append(':')
          }
          append(' ')
          append(filterValueLabel(currentItemFilter))
        },
      )
    },
    trailingContent = {
      Button(
        onClick = onClearClick,
      ) {
        Text("Clear")
      }
    },
    colors = ListItemDefaults.colors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      headlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
      leadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
      trailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
  )
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
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier
            .background(
              color = MaterialTheme.colorScheme.secondaryContainer,
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
  currentItemFilter: LibraryItemFilter?,
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
          selected = currentItemFilter != null && selectedFilter.isValueSelectedFor(currentItemFilter, option),
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
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DropdownMenuItem(
    text = {
      Text(filter.valueLabel(value))
    },
    colors = if (selected) {
      MenuDefaults.itemColors()
    } else {
      MenuDefaults.itemColors()
    },
    trailingIcon = if (selected) {
      { Icon(Icons.Rounded.Check, contentDescription = null) }
    } else {
      null
    },
    onClick = onClick,
    modifier = modifier
      .fluentIf(selected) {
        background(MaterialTheme.colorScheme.primaryContainer)
      },
  )
}

@Composable
private fun filterGroupLabel(
  filter: LibraryItemFilter,
): String = when (filter) {
  is LibraryItemFilter.Authors -> stringResource(Res.string.filter_category_author)
  is LibraryItemFilter.Genres -> stringResource(Res.string.filter_category_genre)
  is LibraryItemFilter.Languages -> stringResource(Res.string.filter_category_language)
  is LibraryItemFilter.Missing -> stringResource(Res.string.filter_category_missing)
  is LibraryItemFilter.Narrators -> stringResource(Res.string.filter_category_narrator)
  is LibraryItemFilter.Progress -> stringResource(Res.string.filter_category_progress)
  is LibraryItemFilter.Series -> stringResource(Res.string.filter_category_series)
  is LibraryItemFilter.Tags -> stringResource(Res.string.filter_category_tag)
  is LibraryItemFilter.Tracks -> stringResource(Res.string.filter_category_tracks)
}

@Composable
private fun filterValueLabel(
  filter: LibraryItemFilter,
): String = when (filter) {
  is LibraryItemFilter.Authors -> filter.authorName
  is LibraryItemFilter.Series -> filter.seriesName

  is LibraryItemFilter.Genres,
  is LibraryItemFilter.Languages,
  is LibraryItemFilter.Narrators,
  is LibraryItemFilter.Tags,
  -> filter.value

  is LibraryItemFilter.Missing -> when (filter.type) {
    LibraryItemFilter.Missing.Type.ASIN -> stringResource(Res.string.filter_value_missing_asin)
    LibraryItemFilter.Missing.Type.ISBN -> stringResource(Res.string.filter_value_missing_isbn)
    LibraryItemFilter.Missing.Type.SUBTITLE -> stringResource(Res.string.filter_value_missing_subtitle)
    LibraryItemFilter.Missing.Type.AUTHORS -> stringResource(Res.string.filter_value_missing_authors)
    LibraryItemFilter.Missing.Type.PUBLISHED_YEAR -> stringResource(Res.string.filter_value_missing_publishedYear)
    LibraryItemFilter.Missing.Type.SERIES -> stringResource(Res.string.filter_value_missing_series)
    LibraryItemFilter.Missing.Type.DESCRIPTION -> stringResource(Res.string.filter_value_missing_description)
    LibraryItemFilter.Missing.Type.GENRES -> stringResource(Res.string.filter_value_missing_genres)
    LibraryItemFilter.Missing.Type.TAGS -> stringResource(Res.string.filter_value_missing_tags)
    LibraryItemFilter.Missing.Type.NARRATORS -> stringResource(Res.string.filter_value_missing_narrators)
    LibraryItemFilter.Missing.Type.PUBLISHER -> stringResource(Res.string.filter_value_missing_publisher)
    LibraryItemFilter.Missing.Type.LANGUAGE -> stringResource(Res.string.filter_value_missing_language)
  }

  is LibraryItemFilter.Progress -> when (filter.type) {
    LibraryItemFilter.Progress.Type.Finished -> stringResource(Res.string.filter_value_progress_finished)
    LibraryItemFilter.Progress.Type.NotStarted -> stringResource(Res.string.filter_value_progress_not_started)
    LibraryItemFilter.Progress.Type.NotFinished -> stringResource(Res.string.filter_value_progress_not_finished)
    LibraryItemFilter.Progress.Type.InProgress -> stringResource(Res.string.filter_value_progress_in_progress)
  }

  is LibraryItemFilter.Tracks -> when (filter.type) {
    LibraryItemFilter.Tracks.Type.Single -> stringResource(Res.string.filter_value_tracks_single)
    LibraryItemFilter.Tracks.Type.Multi -> stringResource(Res.string.filter_value_tracks_multi)
  }
}
