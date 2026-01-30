package app.campfire.filters.ui.sort

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.widgets.SortIcon
import app.campfire.core.di.UserScope
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortModeConfig
import app.campfire.filters.SortModeUi
import campfire.features.filters.ui.generated.resources.Res
import campfire.features.filters.ui.generated.resources.sort_mode_addedat
import campfire.features.filters.ui.generated.resources.sort_mode_author_fl
import campfire.features.filters.ui.generated.resources.sort_mode_author_lf
import campfire.features.filters.ui.generated.resources.sort_mode_duration
import campfire.features.filters.ui.generated.resources.sort_mode_last_book_added
import campfire.features.filters.ui.generated.resources.sort_mode_last_book_updated
import campfire.features.filters.ui.generated.resources.sort_mode_name
import campfire.features.filters.ui.generated.resources.sort_mode_number_of_books
import campfire.features.filters.ui.generated.resources.sort_mode_publishyear
import campfire.features.filters.ui.generated.resources.sort_mode_sheet_title
import campfire.features.filters.ui.generated.resources.sort_mode_size
import campfire.features.filters.ui.generated.resources.sort_mode_title
import campfire.features.filters.ui.generated.resources.sort_mode_updatedat
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.stringResource

sealed interface SortModeResult {
  data object Dismissed : SortModeResult
  data class Selected(val mode: ContentSortMode) : SortModeResult
}

@ContributesBinding(UserScope::class)
@Inject
class OverlaySortModeUi : SortModeUi {

  override suspend fun showContentSortModeBottomSheet(
    overlayHost: OverlayHost,
    current: ContentSortMode,
    currentDirection: SortDirection,
    config: SortModeConfig,
  ): ContentSortMode? {
    val result = overlayHost.showSortModeBottomSheet(current, currentDirection, config)
    return (result as? SortModeResult.Selected)?.mode
  }
}

private suspend fun OverlayHost.showSortModeBottomSheet(
  currentMode: ContentSortMode,
  currentDirection: SortDirection,
  config: SortModeConfig,
): SortModeResult {
  return show(
    BottomSheetOverlay<List<ContentSortMode>, SortModeResult>(
      model = config.availableModes,
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      onDismiss = {
        SortModeResult.Dismissed
      },
      skipPartiallyExpandedState = true,
    ) { modes, overlayNavigator ->
      Impression {
        ScreenViewEvent("SortMode", ScreenType.Overlay)
      }

      SortModeBottomSheet(
        modes = modes,
        currentMode = currentMode,
        currentDirection = currentDirection,
        onModeClick = {
          overlayNavigator.finish(SortModeResult.Selected(it))
        },
      )
    },
  )
}

@Composable
internal fun SortModeBottomSheet(
  modes: List<ContentSortMode>,
  currentMode: ContentSortMode,
  currentDirection: SortDirection,
  onModeClick: (ContentSortMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .navigationBarsPadding(),
  ) {
    Text(
      stringResource(Res.string.sort_mode_sheet_title),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(
        horizontal = 16.dp,
        vertical = 8.dp,
      ),
    )
    modes.forEach { mode ->
      Row(
        modifier = Modifier
          .clickable { onModeClick(mode) }
          .padding(
            horizontal = 16.dp,
            vertical = 16.dp,
          ),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = mode.displayName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = if (currentMode == mode) FontWeight.ExtraBold else FontWeight.Medium,
        )

        Spacer(Modifier.weight(1f))

        if (currentMode == mode) {
          val sortIcon = SortIcon.forMode(currentMode)
          Icon(
            sortIcon.forDirection(currentDirection),
            contentDescription = null,
          )
        }
      }
    }
  }
}

val ContentSortMode.displayName
  @Composable get() = when (this) {
    ContentSortMode.Title -> stringResource(Res.string.sort_mode_title)
    ContentSortMode.AuthorFL -> stringResource(Res.string.sort_mode_author_fl)
    ContentSortMode.AuthorLF -> stringResource(Res.string.sort_mode_author_lf)
    ContentSortMode.PublishYear -> stringResource(Res.string.sort_mode_publishyear)
    ContentSortMode.AddedAt -> stringResource(Res.string.sort_mode_addedat)
    ContentSortMode.Size -> stringResource(Res.string.sort_mode_size)
    ContentSortMode.Duration -> stringResource(Res.string.sort_mode_duration)
    ContentSortMode.UpdatedAt -> stringResource(Res.string.sort_mode_updatedat)
    ContentSortMode.NumberOfBooks -> stringResource(Res.string.sort_mode_number_of_books)
    ContentSortMode.Name -> stringResource(Res.string.sort_mode_name)
    ContentSortMode.LastBookAdded -> stringResource(Res.string.sort_mode_last_book_added)
    ContentSortMode.LastBookUpdated -> stringResource(Res.string.sort_mode_last_book_updated)
  }
