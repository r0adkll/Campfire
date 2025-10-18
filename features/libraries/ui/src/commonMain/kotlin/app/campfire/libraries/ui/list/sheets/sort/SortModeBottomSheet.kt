package app.campfire.libraries.ui.list.sheets.sort

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
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.sort_mode_addedat
import campfire.features.libraries.ui.generated.resources.sort_mode_author_fl
import campfire.features.libraries.ui.generated.resources.sort_mode_author_lf
import campfire.features.libraries.ui.generated.resources.sort_mode_duration
import campfire.features.libraries.ui.generated.resources.sort_mode_publishyear
import campfire.features.libraries.ui.generated.resources.sort_mode_sheet_title
import campfire.features.libraries.ui.generated.resources.sort_mode_size
import campfire.features.libraries.ui.generated.resources.sort_mode_title
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import org.jetbrains.compose.resources.stringResource

sealed interface SortModeResult {
  data object Dismissed : SortModeResult
  data class Selected(val mode: SortMode) : SortModeResult
}

suspend fun OverlayHost.showSortModeBottomSheet(
  currentMode: SortMode,
  currentDirection: SortDirection,
): SortModeResult {
  return show(
    BottomSheetOverlay<List<SortMode>, SortModeResult>(
      model = SortMode.entries.toList(),
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
  modes: List<SortMode>,
  currentMode: SortMode,
  currentDirection: SortDirection,
  onModeClick: (SortMode) -> Unit,
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

val SortMode.displayName
  @Composable get() = when (this) {
    SortMode.Title -> stringResource(Res.string.sort_mode_title)
    SortMode.AuthorFL -> stringResource(Res.string.sort_mode_author_fl)
    SortMode.AuthorLF -> stringResource(Res.string.sort_mode_author_lf)
    SortMode.PublishYear -> stringResource(Res.string.sort_mode_publishyear)
    SortMode.AddedAt -> stringResource(Res.string.sort_mode_addedat)
    SortMode.Size -> stringResource(Res.string.sort_mode_size)
    SortMode.Duration -> stringResource(Res.string.sort_mode_duration)
  }
