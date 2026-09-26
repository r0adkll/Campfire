// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.WifiHome
import app.campfire.ui.settings.SettingsUiEvent.ConnectionSettingEvent
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.SwitchSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_connection_avoid_mobile_subtitle
import campfire.features.settings.ui.generated.resources.setting_connection_avoid_mobile_title
import campfire.features.settings.ui.generated.resources.setting_connection_home_server_header
import org.jetbrains.compose.resources.stringResource

/** For a server with a local address: whether to skip it while on mobile data alone. */
@Composable
internal fun HomeServerSettings(
  avoidMobileData: Boolean,
  onEvent: (ConnectionSettingEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Header(
      title = { Text(stringResource(Res.string.setting_connection_home_server_header)) },
    )

    SwitchSetting(
      value = avoidMobileData,
      onValueChange = { onEvent(ConnectionSettingEvent.AvoidMobileData(it)) },
      headlineContent = { Text(stringResource(Res.string.setting_connection_avoid_mobile_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_connection_avoid_mobile_subtitle)) },
      leadingContent = { Icon(CampfireIcons.Rounded.WifiHome, contentDescription = null) },
    )
  }
}
