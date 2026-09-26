// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import app.campfire.common.compose.extensions.timeAgo
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.Edit
import app.campfire.common.compose.icons.rounded.Lan
import app.campfire.common.compose.icons.rounded.MoreVert
import app.campfire.common.compose.icons.rounded.Wifi
import app.campfire.common.compose.icons.rounded.WifiHome
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.network.reachability.HomeNetwork
import app.campfire.network.reachability.NetworkTransport
import app.campfire.ui.settings.HomeNetworkSettingsInfo
import app.campfire.ui.settings.SettingsUiEvent.ConnectionSettingEvent
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.SettingListItem
import app.campfire.ui.settings.composables.SwitchSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.dialog_confirm_action
import campfire.features.settings.ui.generated.resources.dialog_dismiss_action
import campfire.features.settings.ui.generated.resources.setting_connection_away_header
import campfire.features.settings.ui.generated.resources.setting_connection_home_network_current
import campfire.features.settings.ui.generated.resources.setting_connection_home_network_ethernet
import campfire.features.settings.ui.generated.resources.setting_connection_home_network_forget
import campfire.features.settings.ui.generated.resources.setting_connection_home_network_last_seen
import campfire.features.settings.ui.generated.resources.setting_connection_home_network_options
import campfire.features.settings.ui.generated.resources.setting_connection_home_network_rename
import campfire.features.settings.ui.generated.resources.setting_connection_home_network_rename_label
import campfire.features.settings.ui.generated.resources.setting_connection_home_network_rename_title
import campfire.features.settings.ui.generated.resources.setting_connection_home_network_wifi
import campfire.features.settings.ui.generated.resources.setting_connection_home_networks_empty
import campfire.features.settings.ui.generated.resources.setting_connection_home_networks_forget_all
import campfire.features.settings.ui.generated.resources.setting_connection_home_networks_header
import campfire.features.settings.ui.generated.resources.setting_connection_pause_away_subtitle
import campfire.features.settings.ui.generated.resources.setting_connection_pause_away_title
import org.jetbrains.compose.resources.stringResource

/**
 * The away-from-home switch and, for a server with a local address, the networks Campfire has
 * learned it can be reached from.
 */
@Composable
internal fun HomeNetworkSettings(
  info: HomeNetworkSettingsInfo,
  onEvent: (ConnectionSettingEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Header(
      title = { Text(stringResource(Res.string.setting_connection_away_header)) },
    )

    SwitchSetting(
      value = info.pauseAwayFromHome,
      onValueChange = { onEvent(ConnectionSettingEvent.PauseAwayFromHome(it)) },
      headlineContent = { Text(stringResource(Res.string.setting_connection_pause_away_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_connection_pause_away_subtitle)) },
      leadingContent = { Icon(CampfireIcons.Rounded.WifiHome, contentDescription = null) },
    )

    if (!info.isLocalServer || !info.pauseAwayFromHome) return@Column

    Header(
      title = { Text(stringResource(Res.string.setting_connection_home_networks_header)) },
    )

    if (info.networks.isEmpty()) {
      SettingListItem(
        headlineContent = { Text(stringResource(Res.string.setting_connection_home_networks_empty)) },
      )
      return@Column
    }

    info.networks.forEach { network ->
      HomeNetworkItem(
        network = network,
        onRename = { label -> onEvent(ConnectionSettingEvent.RenameHomeNetwork(network.key, label)) },
        onForget = { onEvent(ConnectionSettingEvent.ForgetHomeNetwork(network.key)) },
      )
    }

    ActionSetting(
      headlineContent = { Text(stringResource(Res.string.setting_connection_home_networks_forget_all)) },
      leadingContent = { Icon(CampfireIcons.Rounded.Delete, contentDescription = null) },
      onClick = { onEvent(ConnectionSettingEvent.ForgetAllHomeNetworks) },
    )
  }
}

@Composable
private fun HomeNetworkItem(
  network: HomeNetwork,
  onRename: (String) -> Unit,
  onForget: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showMenu by remember { mutableStateOf(false) }
  var showRename by remember { mutableStateOf(false) }

  val transportLabel = stringResource(
    when (network.transport) {
      NetworkTransport.Ethernet -> Res.string.setting_connection_home_network_ethernet
      else -> Res.string.setting_connection_home_network_wifi
    },
  )
  // The subnet is only shown as the headline of an unnamed network, where it's what tells networks
  // apart; the gateway and domain are in the developer diagnostics
  val details = listOf(
    transportLabel,
    stringResource(Res.string.setting_connection_home_network_last_seen, network.lastSeenAtMs.timeAgo),
  ).joinToString(" · ")

  SettingListItem(
    headlineContent = { Text(network.label ?: network.subnet) },
    overlineContent = if (network.isCurrent) {
      {
        Text(
          text = stringResource(Res.string.setting_connection_home_network_current),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    } else {
      null
    },
    supportingContent = { Text(details) },
    leadingContent = {
      Icon(
        imageVector = when (network.transport) {
          NetworkTransport.Ethernet -> CampfireIcons.Rounded.Lan
          else -> CampfireIcons.Rounded.Wifi
        },
        contentDescription = transportLabel,
      )
    },
    trailingContent = {
      Box {
        val optionsLabel = stringResource(Res.string.setting_connection_home_network_options)
        IconButtonTooltip(text = optionsLabel) {
          IconButton(onClick = { showMenu = true }) {
            Icon(CampfireIcons.Rounded.MoreVert, contentDescription = optionsLabel)
          }
        }
        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(Res.string.setting_connection_home_network_rename)) },
            leadingIcon = { Icon(CampfireIcons.Rounded.Edit, contentDescription = null) },
            onClick = {
              showMenu = false
              showRename = true
            },
          )
          DropdownMenuItem(
            text = { Text(stringResource(Res.string.setting_connection_home_network_forget)) },
            leadingIcon = { Icon(CampfireIcons.Rounded.Delete, contentDescription = null) },
            onClick = {
              showMenu = false
              onForget()
            },
          )
        }
      }
    },
    modifier = modifier,
  )

  if (showRename) {
    RenameNetworkDialog(
      initial = network.label.orEmpty(),
      onConfirm = { label ->
        onRename(label)
        showRename = false
      },
      onDismiss = { showRename = false },
    )
  }
}

@Composable
private fun RenameNetworkDialog(
  initial: String,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var text by remember {
    mutableStateOf(TextFieldValue(text = initial, selection = TextRange(0, initial.length)))
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(CampfireIcons.Rounded.WifiHome, contentDescription = null) },
    title = { Text(stringResource(Res.string.setting_connection_home_network_rename_title)) },
    text = {
      TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(Res.string.setting_connection_home_network_rename_label)) },
        singleLine = true,
        // The keyboard can cover the dialog buttons (e.g. in landscape), so Done confirms too
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onConfirm(text.text) }),
      )
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(text.text) }) {
        Text(stringResource(Res.string.dialog_confirm_action))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.dialog_dismiss_action))
      }
    },
    modifier = modifier,
  )
}
