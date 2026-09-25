// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.ui.settings.LocalNetworkAccess
import app.campfire.ui.settings.SettingsUiEvent.ConnectionSettingEvent
import app.campfire.ui.settings.composables.SettingListItem
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_connection_local_network_allow
import campfire.features.settings.ui.generated.resources.setting_connection_local_network_denied
import campfire.features.settings.ui.generated.resources.setting_connection_local_network_missing
import campfire.features.settings.ui.generated.resources.setting_connection_local_network_open_settings
import campfire.features.settings.ui.generated.resources.setting_connection_local_network_title
import org.jetbrains.compose.resources.stringResource

/**
 * Surfaces Android 17's local network permission when it's blocking a server on the home network:
 * without it the OS silently drops every connection, so nothing else in the app can work.
 */
@Composable
internal fun LocalNetworkAccessSetting(
  access: LocalNetworkAccess,
  onEvent: (ConnectionSettingEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingListItem(
    headlineContent = { Text(stringResource(Res.string.setting_connection_local_network_title)) },
    supportingContent = {
      Text(
        stringResource(
          when (access) {
            LocalNetworkAccess.Missing -> Res.string.setting_connection_local_network_missing
            LocalNetworkAccess.Denied -> Res.string.setting_connection_local_network_denied
          },
        ),
      )
    },
    leadingContent = {
      Icon(
        imageVector = CampfireIcons.Rounded.Warning,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
      )
    },
    trailingContent = {
      FilledTonalButton(
        onClick = {
          onEvent(
            when (access) {
              LocalNetworkAccess.Missing -> ConnectionSettingEvent.AllowLocalNetwork
              LocalNetworkAccess.Denied -> ConnectionSettingEvent.OpenAppSettings
            },
          )
        },
      ) {
        Text(
          stringResource(
            when (access) {
              LocalNetworkAccess.Missing -> Res.string.setting_connection_local_network_allow
              LocalNetworkAccess.Denied -> Res.string.setting_connection_local_network_open_settings
            },
          ),
        )
      }
    },
    modifier = modifier,
  )
}
