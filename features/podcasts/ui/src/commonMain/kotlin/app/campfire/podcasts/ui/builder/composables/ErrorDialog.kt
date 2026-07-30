// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.builder.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.campfire.podcasts.ui.builder.SubmitError
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_error_conflict
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_error_dismiss
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_error_forbidden
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_error_generic
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ErrorDialog(error: SubmitError, onDismiss: () -> Unit) {
  val message = when (error) {
    SubmitError.Forbidden -> stringResource(Res.string.add_podcast_builder_error_forbidden)
    SubmitError.PathConflict -> stringResource(Res.string.add_podcast_builder_error_conflict)
    SubmitError.Generic -> stringResource(Res.string.add_podcast_builder_error_generic)
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    text = { Text(message) },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.add_podcast_builder_error_dismiss))
      }
    },
  )
}
