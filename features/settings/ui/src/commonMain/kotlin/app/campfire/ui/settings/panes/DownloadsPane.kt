// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ChevronRight
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.SwitchSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.download_header_downloads
import campfire.features.settings.ui.generated.resources.download_header_episodes
import campfire.features.settings.ui.generated.resources.setting_delete_finished_episodes_description
import campfire.features.settings.ui.generated.resources.setting_delete_finished_episodes_title
import campfire.features.settings.ui.generated.resources.setting_download_on_wifi_only_description
import campfire.features.settings.ui.generated.resources.setting_download_on_wifi_only_title
import campfire.features.settings.ui.generated.resources.setting_download_queued_episodes_description
import campfire.features.settings.ui.generated.resources.setting_download_queued_episodes_title
import campfire.features.settings.ui.generated.resources.setting_downloads_title
import campfire.features.settings.ui.generated.resources.setting_show_download_confirmation_description
import campfire.features.settings.ui.generated.resources.setting_show_download_confirmation_title
import campfire.features.settings.ui.generated.resources.setting_view_all_downloads_count
import campfire.features.settings.ui.generated.resources.setting_view_all_downloads_none
import campfire.features.settings.ui.generated.resources.setting_view_all_downloads_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DownloadsPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_downloads_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    SwitchSetting(
      value = state.downloadsSettings.showDownloadConfirmation,
      onValueChange = { state.eventSink(SettingsUiEvent.DownloadsSettingEvent.ShowDownloadConfirmation(it)) },
      headlineContent = { Text(stringResource(Res.string.setting_show_download_confirmation_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_show_download_confirmation_description)) },
    )

    if (state.downloadsSettings.isWifiOnlyAvailable) {
      SwitchSetting(
        value = state.downloadsSettings.downloadOnWifiOnly,
        onValueChange = { state.eventSink(SettingsUiEvent.DownloadsSettingEvent.DownloadOnWifiOnly(it)) },
        headlineContent = { Text(stringResource(Res.string.setting_download_on_wifi_only_title)) },
        supportingContent = { Text(stringResource(Res.string.setting_download_on_wifi_only_description)) },
      )
    }

    if (state.downloadsSettings.isAudioDownloadAvailable) {
      Header(title = { Text(stringResource(Res.string.download_header_episodes)) })

      SwitchSetting(
        value = state.downloadsSettings.downloadQueuedEpisodes,
        onValueChange = { state.eventSink(SettingsUiEvent.DownloadsSettingEvent.DownloadQueuedEpisodes(it)) },
        headlineContent = { Text(stringResource(Res.string.setting_download_queued_episodes_title)) },
        supportingContent = { Text(stringResource(Res.string.setting_download_queued_episodes_description)) },
      )

      SwitchSetting(
        value = state.downloadsSettings.deleteFinishedEpisodes,
        onValueChange = { state.eventSink(SettingsUiEvent.DownloadsSettingEvent.DeleteFinishedEpisodes(it)) },
        headlineContent = { Text(stringResource(Res.string.setting_delete_finished_episodes_title)) },
        supportingContent = { Text(stringResource(Res.string.setting_delete_finished_episodes_description)) },
      )
    }

    Header(title = { Text(stringResource(Res.string.download_header_downloads)) })

    val downloadCount = state.downloadsSettings.downloadCount
    ActionSetting(
      headlineContent = { Text(stringResource(Res.string.setting_view_all_downloads_title)) },
      supportingContent = {
        Text(
          if (downloadCount == 0) {
            stringResource(Res.string.setting_view_all_downloads_none)
          } else {
            pluralStringResource(Res.plurals.setting_view_all_downloads_count, downloadCount, downloadCount)
          },
        )
      },
      trailingContent = { Icon(CampfireIcons.Rounded.ChevronRight, contentDescription = null) },
      onClick = { state.eventSink(SettingsUiEvent.DownloadsSettingEvent.ViewAllDownloads) },
    )
  }
}
