package app.campfire.ui.settings.panes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.SwitchSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_downloads_title
import campfire.features.settings.ui.generated.resources.setting_show_download_confirmation_description
import campfire.features.settings.ui.generated.resources.setting_show_download_confirmation_title
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
  }
}
