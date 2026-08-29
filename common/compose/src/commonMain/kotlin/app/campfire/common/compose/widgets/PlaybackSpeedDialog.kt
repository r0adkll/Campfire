// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import app.campfire.core.extensions.readableHundredths
import app.campfire.core.extensions.roundToHundredths
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.playback_speed_dialog_apply
import campfire.common.compose.generated.resources.playback_speed_dialog_cancel
import campfire.common.compose.generated.resources.playback_speed_dialog_error
import campfire.common.compose.generated.resources.playback_speed_dialog_label
import campfire.common.compose.generated.resources.playback_speed_dialog_title
import org.jetbrains.compose.resources.stringResource

/** The supported range for user-adjusted playback speeds. */
val PlaybackSpeedRange: ClosedFloatingPointRange<Float> = 0.5f.rangeTo(3f)

/**
 * A dialog for typing an exact playback speed, validated against [range].
 */
@Composable
fun PlaybackSpeedDialog(
  initialSpeed: Float,
  onDismiss: () -> Unit,
  onConfirm: (Float) -> Unit,
  modifier: Modifier = Modifier,
  range: ClosedFloatingPointRange<Float> = PlaybackSpeedRange,
) {
  var input by remember { mutableStateOf(initialSpeed.readableHundredths) }
  val parsed = parsePlaybackSpeed(input, range)
  val isError = input.isNotBlank() && parsed == null
  val rangeError = stringResource(
    Res.string.playback_speed_dialog_error,
    range.start.readableHundredths,
    range.endInclusive.readableHundredths,
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = modifier,
    title = { Text(stringResource(Res.string.playback_speed_dialog_title)) },
    text = {
      OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        singleLine = true,
        isError = isError,
        label = { Text(stringResource(Res.string.playback_speed_dialog_label)) },
        suffix = { Text("x") },
        supportingText = { Text(rangeError) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      )
    },
    confirmButton = {
      TextButton(
        enabled = parsed != null,
        onClick = { parsed?.let(onConfirm) },
      ) {
        Text(stringResource(Res.string.playback_speed_dialog_apply))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.playback_speed_dialog_cancel))
      }
    },
  )
}

/**
 * Parse a user-entered playback speed such as "1.2", "1,2" or "1.2x", rounded to hundredths.
 * Returns null if the text isn't a number or falls outside [range].
 */
fun parsePlaybackSpeed(
  text: String,
  range: ClosedFloatingPointRange<Float> = PlaybackSpeedRange,
): Float? {
  val value = text.trim()
    .removeSuffix("x")
    .removeSuffix("X")
    .trim()
    .replace(',', '.')
    .toFloatOrNull()
    ?.roundToHundredths()
    ?: return null
  return value.takeIf { it in range }
}
