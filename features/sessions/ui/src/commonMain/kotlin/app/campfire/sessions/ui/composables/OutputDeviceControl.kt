// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Check
import app.campfire.common.compose.icons.rounded.Speaker
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.sessions.ui.playback.OutputDeviceUiEvent
import app.campfire.sessions.ui.playback.OutputDeviceUiState
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_output_device
import campfire.features.sessions.ui.generated.resources.output_device_system_default
import campfire.features.sessions.ui.generated.resources.output_device_unavailable
import org.jetbrains.compose.resources.stringResource

/**
 * Picks which output playback goes to. Only rendered where routing is possible at all — see
 * [app.campfire.audioplayer.AudioOutputController.supportsDeviceSelection].
 *
 * The list is read when the menu opens rather than polled. There is nothing to subscribe to, and
 * polling would not help: Java rebuilds its device list only when the device *count* changes, so
 * a same-count swap leaves stale names however often you ask. Nothing in-process can force a
 * refresh, which is why there is no "refresh" item here — it would do nothing.
 */
@Composable
internal fun OutputDeviceControl(
  state: OutputDeviceUiState,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val label = stringResource(Res.string.action_output_device)

  Box(modifier) {
    IconButtonTooltip(text = label) {
      IconButton(
        onClick = {
          state.eventSink(OutputDeviceUiEvent.Refresh)
          expanded = true
        },
      ) {
        Icon(CampfireIcons.Rounded.Speaker, contentDescription = label)
      }
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      OutputDeviceMenuItems(state, onChosen = { expanded = false })
    }
  }
}

/**
 * The device choices themselves, so the standalone picker and the bar's overflow menu offer the
 * same list rather than one nesting a menu inside the other.
 */
@Composable
internal fun OutputDeviceMenuItems(
  state: OutputDeviceUiState,
  onChosen: () -> Unit,
) {
  val systemDefaultLabel = stringResource(Res.string.output_device_system_default)
  val unavailableLabel = stringResource(Res.string.output_device_unavailable)

  // Synthesised rather than listed by the engine: following the system default is the absence of
  // a pin, and it is the one route that keeps tracking the OS while playing.
  DropdownMenuItem(
    text = { Text(systemDefaultLabel) },
    trailingIcon = selectedMark(state.selectedName == null),
    onClick = {
      onChosen()
      state.eventSink(OutputDeviceUiEvent.SelectDevice(null))
    },
  )

  state.devices.forEach { device ->
    DropdownMenuItem(
      text = { Text(device.name) },
      trailingIcon = selectedMark(device.name == state.selectedName),
      onClick = {
        onChosen()
        state.eventSink(OutputDeviceUiEvent.SelectDevice(device))
      },
    )
  }

  // The pinned device is gone — playback fell back to the system default. Shown disabled so both
  // facts are visible: what was chosen, and that it is not what is playing.
  if (state.selectedIsMissing && state.selectedName != null) {
    DropdownMenuItem(
      text = { Text(state.selectedName) },
      trailingIcon = { Text(unavailableLabel) },
      enabled = false,
      onClick = {},
    )
  }
}

private fun selectedMark(selected: Boolean): (@Composable () -> Unit)? =
  if (selected) {
    { Icon(CampfireIcons.Rounded.Check, contentDescription = null) }
  } else {
    null
  }
