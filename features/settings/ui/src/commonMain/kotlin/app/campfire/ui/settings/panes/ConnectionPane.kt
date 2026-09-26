// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Sync
import app.campfire.ui.settings.SettingsUiEvent.ConnectionSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.SwitchSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_connection_realtime_sync_header
import campfire.features.settings.ui.generated.resources.setting_connection_socket_sync_subtitle
import campfire.features.settings.ui.generated.resources.setting_connection_socket_sync_title
import campfire.features.settings.ui.generated.resources.setting_connection_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConnectionPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_connection_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    state.localNetworkAccess?.let { access ->
      LocalNetworkAccessSetting(
        access = access,
        onEvent = state.eventSink,
      )
    }

    Header(
      title = { Text(stringResource(Res.string.setting_connection_realtime_sync_header)) },
    )

    SwitchSetting(
      value = state.socketSyncEnabled,
      onValueChange = {
        state.eventSink(ConnectionSettingEvent.SocketSyncEnabled(it))
      },
      headlineContent = { Text(stringResource(Res.string.setting_connection_socket_sync_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_connection_socket_sync_subtitle)) },
      leadingContent = { Icon(CampfireIcons.Rounded.Sync, contentDescription = null) },
    )

    if (state.homeServerSettings.isVisible) {
      HomeServerSettings(
        avoidMobileData = state.homeServerSettings.avoidMobileData,
        onEvent = state.eventSink,
      )
    }

    CustomHeaderSettings(
      headers = state.customHeaders,
      onEvent = state.eventSink,
    )
  }
}
