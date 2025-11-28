package app.campfire.ui.settings.panes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.Platform
import app.campfire.core.coroutines.onLoaded
import app.campfire.core.currentPlatform
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.SwitchSetting
import app.campfire.ui.settings.composables.TentSetting
import app.campfire.ui.settings.composables.ThemeModeSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.header_appearance_dynamic
import campfire.features.settings.ui.generated.resources.header_appearance_overall
import campfire.features.settings.ui.generated.resources.setting_appearance_title
import campfire.features.settings.ui.generated.resources.setting_dynamic_colors_description
import campfire.features.settings.ui.generated.resources.setting_dynamic_colors_title
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

    state.server.onLoaded { server ->
      TentSetting(
        tent = server.tent,
        enabled = !state.appearanceSettings.useDynamicColors,
        onTentChange = { state.eventSink(SettingsUiEvent.AccountSettingEvent.ChangeTent(it)) },
      )
    }

    ThemeModeSetting(
      themeMode = state.appearanceSettings.theme,
      onThemeChange = { state.eventSink(SettingsUiEvent.AppearanceSettingEvent.Theme(it)) },
    )

    if (currentPlatform == Platform.ANDROID) {
      SwitchSetting(
        value = state.appearanceSettings.useDynamicColors,
        onValueChange = { state.eventSink(SettingsUiEvent.AppearanceSettingEvent.UseDynamicColors(it)) },
        headlineContent = { Text(stringResource(Res.string.setting_dynamic_colors_title)) },
        supportingContent = { Text(stringResource(Res.string.setting_dynamic_colors_description)) },
      )
    }

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
