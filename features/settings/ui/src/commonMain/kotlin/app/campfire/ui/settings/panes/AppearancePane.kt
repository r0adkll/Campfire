package app.campfire.ui.settings.panes

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.SwitchSetting
import app.campfire.ui.settings.composables.ThemeModeSetting
import app.campfire.ui.theming.api.AppThemeImage
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.header_appearance_dynamic
import campfire.features.settings.ui.generated.resources.header_appearance_overall
import campfire.features.settings.ui.generated.resources.setting_appearance_title
import campfire.features.settings.ui.generated.resources.setting_dynamic_item_detail_description
import campfire.features.settings.ui.generated.resources.setting_dynamic_item_detail_title
import campfire.features.settings.ui.generated.resources.setting_dynamic_playback_description
import campfire.features.settings.ui.generated.resources.setting_dynamic_playback_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppearancePane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_appearance_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    Header(
      title = { Text(stringResource(Res.string.header_appearance_overall)) },
    )

    ActionSetting(
      headlineContent = { Text("Theme") },
      supportingContent = { Text("Change or customize the application appearance") },
      trailingContent = {
        AppThemeImage(
          appTheme = state.appearanceSettings.appTheme,
          modifier = Modifier.size(48.dp),
        )
      },
      onClick = {
        state.eventSink(SettingsUiEvent.AppearanceSettingEvent.OpenThemeBuilder)
      },
    )

    ThemeModeSetting(
      themeMode = state.appearanceSettings.themeMode,
      onThemeChange = { state.eventSink(SettingsUiEvent.AppearanceSettingEvent.Theme(it)) },
    )

    Header(
      title = { Text(stringResource(Res.string.header_appearance_dynamic)) },
    )

    SwitchSetting(
      value = state.appearanceSettings.dynamicItemDetailTheming,
      onValueChange = { state.eventSink(SettingsUiEvent.AppearanceSettingEvent.DynamicItemDetailTheming(it)) },
      headlineContent = { Text(stringResource(Res.string.setting_dynamic_item_detail_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_dynamic_item_detail_description)) },
    )

    SwitchSetting(
      value = state.appearanceSettings.dynamicPlaybackTheming,
      onValueChange = { state.eventSink(SettingsUiEvent.AppearanceSettingEvent.DynamicPlaybackTheming(it)) },
      headlineContent = { Text(stringResource(Res.string.setting_dynamic_playback_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_dynamic_playback_description)) },
    )
  }
}
