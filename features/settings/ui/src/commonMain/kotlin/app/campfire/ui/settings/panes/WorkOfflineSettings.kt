// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Cloud
import app.campfire.common.compose.icons.rounded.CloudOff
import app.campfire.common.compose.icons.rounded.SignalCellularAlt
import app.campfire.common.compose.offline.goOnlineLabel
import app.campfire.common.compose.offline.label
import app.campfire.common.compose.offline.offlineChoices
import app.campfire.common.compose.offline.statusText
import app.campfire.network.reachability.OfflineChoice
import app.campfire.network.reachability.OfflineState
import app.campfire.network.reachability.Reachability
import app.campfire.ui.settings.SettingsUiEvent.ConnectionSettingEvent
import app.campfire.ui.settings.composables.SwitchSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_connection_offline_detail
import campfire.features.settings.ui.generated.resources.setting_connection_online_connected
import campfire.features.settings.ui.generated.resources.setting_connection_online_server
import campfire.features.settings.ui.generated.resources.setting_connection_online_title
import campfire.features.settings.ui.generated.resources.setting_connection_online_unreachable
import campfire.features.settings.ui.generated.resources.setting_connection_server_fallback
import campfire.features.settings.ui.generated.resources.setting_connection_work_offline_action
import campfire.features.settings.ui.generated.resources.setting_connection_work_offline_dialog_cancel
import campfire.features.settings.ui.generated.resources.setting_connection_work_offline_dialog_confirm
import campfire.features.settings.ui.generated.resources.setting_connection_work_offline_mobile_data_subtitle
import campfire.features.settings.ui.generated.resources.setting_connection_work_offline_mobile_data_title
import campfire.features.settings.ui.generated.resources.setting_connection_work_offline_title
import org.jetbrains.compose.resources.stringResource

/**
 * Working offline: a card saying whether Campfire is online or why it's offline, with the one
 * action that changes it — "Work offline…" (choosing how long in a dialog) or "Go online now" —
 * then the standing mobile data rule where the device can tell Wi-Fi from mobile data.
 */
@Composable
internal fun WorkOfflineSettings(
  offline: OfflineState,
  canDetectWifi: Boolean,
  workOfflineOnMobileData: Boolean,
  serverName: String?,
  reachability: Reachability,
  onEvent: (ConnectionSettingEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  var choosing by remember { mutableStateOf(false) }

  Column(modifier) {
    ConnectionStatusCard(
      offline = offline,
      serverName = serverName ?: stringResource(Res.string.setting_connection_server_fallback),
      reachability = reachability,
      onWorkOffline = { choosing = true },
      onGoOnline = { onEvent(ConnectionSettingEvent.GoOnline) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    )

    if (canDetectWifi) {
      SwitchSetting(
        value = workOfflineOnMobileData,
        onValueChange = { onEvent(ConnectionSettingEvent.WorkOfflineOnMobileData(it)) },
        headlineContent = { Text(stringResource(Res.string.setting_connection_work_offline_mobile_data_title)) },
        supportingContent = {
          Text(stringResource(Res.string.setting_connection_work_offline_mobile_data_subtitle))
        },
        leadingContent = { Icon(CampfireIcons.Rounded.SignalCellularAlt, contentDescription = null) },
      )
    }
  }

  if (choosing) {
    WorkOfflineDialog(
      choices = offlineChoices(canDetectWifi),
      onWifi = offline.onWifi,
      onConfirm = { choice ->
        choosing = false
        onEvent(ConnectionSettingEvent.GoOffline(choice))
      },
      onDismissRequest = { choosing = false },
    )
  }
}

@Composable
private fun ConnectionStatusCard(
  offline: OfflineState,
  serverName: String,
  reachability: Reachability,
  onWorkOffline: () -> Unit,
  onGoOnline: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val reason = offline.reason
  val containerColor by animateColorAsState(
    if (reason != null) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
  )
  val contentColor by animateColorAsState(
    if (reason != null) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
  )

  Card(
    colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    shape = MaterialTheme.shapes.large,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          if (reason != null) CampfireIcons.Rounded.CloudOff else CampfireIcons.Rounded.Cloud,
          contentDescription = null,
        )
        Text(
          text = reason?.statusText() ?: stringResource(Res.string.setting_connection_online_title),
          style = MaterialTheme.typography.titleMedium,
        )
      }

      Text(
        text = when {
          reason != null -> stringResource(Res.string.setting_connection_offline_detail)
          reachability == Reachability.Reachable -> stringResource(
            Res.string.setting_connection_online_connected,
            serverName,
          )
          reachability == Reachability.Unreachable || reachability == Reachability.OutOfRange ->
            stringResource(Res.string.setting_connection_online_unreachable, serverName)
          else -> stringResource(Res.string.setting_connection_online_server, serverName)
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 36.dp),
      )

      Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
      ) {
        FilledTonalButton(
          onClick = if (reason != null) onGoOnline else onWorkOffline,
        ) {
          Text(reason?.goOnlineLabel() ?: stringResource(Res.string.setting_connection_work_offline_action))
        }
      }
    }
  }
}

@Composable
private fun WorkOfflineDialog(
  choices: List<OfflineChoice>,
  onWifi: Boolean,
  onConfirm: (OfflineChoice) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selected by remember { mutableStateOf(OfflineChoice.UntilTurnedOff) }

  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(Res.string.setting_connection_work_offline_title)) },
    text = {
      Column(Modifier.selectableGroup()) {
        choices.forEach { choice ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .selectable(
                selected = choice == selected,
                onClick = { selected = choice },
                role = Role.RadioButton,
              )
              .padding(vertical = 4.dp),
          ) {
            RadioButton(selected = choice == selected, onClick = null)
            Text(
              text = choice.label(onWifi = onWifi),
              style = MaterialTheme.typography.bodyLarge,
              modifier = Modifier.padding(start = 16.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(selected) }) {
        Text(stringResource(Res.string.setting_connection_work_offline_dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(Res.string.setting_connection_work_offline_dialog_cancel))
      }
    },
    modifier = modifier,
  )
}
