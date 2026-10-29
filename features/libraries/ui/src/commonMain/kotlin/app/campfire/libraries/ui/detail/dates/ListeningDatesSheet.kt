// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Edit
import app.campfire.common.compose.icons.rounded.Event
import app.campfire.common.compose.icons.rounded.EventAvailable
import app.campfire.common.compose.icons.rounded.Info
import app.campfire.common.compose.widgets.TonalIcon
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.common.compose.widgets.sheets.LocalSheetPresentation
import app.campfire.common.compose.widgets.sheets.SheetPresentation
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.readableFormat
import app.campfire.core.model.MediaProgress
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.listening_dates_edit
import campfire.features.libraries.ui.generated.resources.listening_dates_finished
import campfire.features.libraries.ui.generated.resources.listening_dates_saved_to_account
import campfire.features.libraries.ui.generated.resources.listening_dates_started
import campfire.features.libraries.ui.generated.resources.listening_dates_title
import campfire.features.libraries.ui.generated.resources.listening_dates_unknown
import com.slack.circuit.overlay.OverlayHost
import org.jetbrains.compose.resources.stringResource

/**
 * Shows when [progress] was started and finished, and returns the date picked to change, or null
 * when the sheet is put away without one.
 */
suspend fun OverlayHost.showListeningDatesSheet(
  itemTitle: String?,
  progress: MediaProgress,
): ListeningDate? {
  val result = show(
    AdaptiveSheetOverlay<MediaProgress, ListeningDatesResult>(
      model = progress,
      onDismiss = { ListeningDatesResult.Dismissed },
      skipPartiallyExpanded = true,
    ) { model, navigator ->
      Impression {
        ScreenViewEvent("ListeningDates", ScreenType.Overlay)
      }

      ListeningDatesSheet(
        itemTitle = itemTitle,
        progress = model,
        onDateClick = { navigator.finish(ListeningDatesResult.Edit(it)) },
      )
    },
  )
  return (result as? ListeningDatesResult.Edit)?.date
}

private sealed interface ListeningDatesResult {
  data object Dismissed : ListeningDatesResult
  data class Edit(val date: ListeningDate) : ListeningDatesResult
}

/**
 * The dates [progress] keeps, each with a way to change it where it can be. Audiobookshelf only
 * lets a progress's start date be set when the progress is first made, so it shows without one.
 */
@Composable
internal fun ListeningDatesSheet(
  itemTitle: String?,
  progress: MediaProgress,
  onDateClick: (ListeningDate) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isSideSheet = LocalSheetPresentation.current == SheetPresentation.Side
  Column(
    modifier = modifier
      .then(if (isSideSheet) Modifier.width(SideSheetWidth) else Modifier.fillMaxWidth())
      .padding(bottom = 24.dp),
  ) {
    Column(
      modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = stringResource(Res.string.listening_dates_title),
        style = MaterialTheme.typography.headlineSmall,
      )
      if (itemTitle != null) {
        Text(
          text = itemTitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    ListeningDateRow(
      label = stringResource(Res.string.listening_dates_started),
      date = progress.startedAt.takeIf { it > 0L },
      icon = {
        TonalIcon(
          icon = CampfireIcons.Rounded.Event,
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          contentDescription = null,
        )
      },
      onClick = null,
      modifier = Modifier.testTag("listening_date_started"),
    )

    ListeningDateRow(
      label = stringResource(Res.string.listening_dates_finished),
      date = progress.finishedAt,
      icon = {
        TonalIcon(
          icon = CampfireIcons.Rounded.EventAvailable,
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentDescription = null,
        )
      },
      onClick = { onDateClick(ListeningDate.Finished) },
      modifier = Modifier.testTag("listening_date_finished"),
    )

    Row(
      modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        CampfireIcons.Rounded.Info,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = stringResource(Res.string.listening_dates_saved_to_account),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(Modifier.height(8.dp))
    Spacer(Modifier.navigationBarsPadding())
  }
}

@Composable
private fun ListeningDateRow(
  label: String,
  date: Long?,
  icon: @Composable () -> Unit,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val editLabel = stringResource(Res.string.listening_dates_edit)
  ListItem(
    overlineContent = { Text(label) },
    headlineContent = {
      Text(date?.asDate()?.readableFormat ?: stringResource(Res.string.listening_dates_unknown))
    },
    leadingContent = icon,
    trailingContent = if (onClick != null) {
      {
        Icon(
          CampfireIcons.Rounded.Edit,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      null
    },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = modifier
      .then(
        if (onClick != null) {
          Modifier.clickable(onClickLabel = editLabel, role = Role.Button, onClick = onClick)
        } else {
          Modifier
        },
      )
      .padding(start = 8.dp),
  )
}

private val SideSheetWidth = 360.dp
