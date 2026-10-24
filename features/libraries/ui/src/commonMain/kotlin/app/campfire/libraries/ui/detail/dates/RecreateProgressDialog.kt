// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ArrowForward
import app.campfire.common.compose.icons.rounded.Check
import app.campfire.common.compose.icons.rounded.History
import app.campfire.core.extensions.readableFormat
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.listening_dates_started
import campfire.features.libraries.ui.generated.resources.listening_dates_unknown
import campfire.features.libraries.ui.generated.resources.recreate_progress_cancel
import campfire.features.libraries.ui.generated.resources.recreate_progress_confirm
import campfire.features.libraries.ui.generated.resources.recreate_progress_kept
import campfire.features.libraries.ui.generated.resources.recreate_progress_message
import campfire.features.libraries.ui.generated.resources.recreate_progress_other_devices
import campfire.features.libraries.ui.generated.resources.recreate_progress_title
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before changing when a progress started, which Audiobookshelf only allows by deleting the
 * progress and creating it again, and says what that keeps.
 */
@Composable
internal fun RecreateProgressDialog(
  from: LocalDate?,
  to: LocalDate,
  onConfirm: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    icon = {
      Icon(
        CampfireIcons.Rounded.History,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
      )
    },
    title = { Text(stringResource(Res.string.recreate_progress_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(Res.string.recreate_progress_message))

        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
          shape = MaterialTheme.shapes.large,
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = stringResource(Res.string.listening_dates_started),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              // Two long dates don't fit a dialog's width side by side, so each wraps whole
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = from?.readableFormat ?: stringResource(Res.string.listening_dates_unknown),
                  style = MaterialTheme.typography.bodyMedium,
                  textDecoration = TextDecoration.LineThrough,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  softWrap = false,
                )
                Icon(
                  CampfireIcons.Rounded.ArrowForward,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = to.readableFormat,
                  style = MaterialTheme.typography.labelLarge,
                  softWrap = false,
                  modifier = Modifier.testTag("recreate_progress_new_start"),
                )
              }
            }
            HorizontalDivider()
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                CampfireIcons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = stringResource(Res.string.recreate_progress_kept),
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }

        Text(
          text = stringResource(Res.string.recreate_progress_other_devices),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        modifier = Modifier.testTag("recreate_progress_confirm"),
      ) {
        Text(stringResource(Res.string.recreate_progress_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(Res.string.recreate_progress_cancel))
      }
    },
  )
}
