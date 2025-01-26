package app.campfire.ui.settings.panes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.Platform
import app.campfire.core.currentPlatform
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.SwitchSetting
import app.campfire.ui.settings.composables.ThemeModeSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_appearance_title
import campfire.features.settings.ui.generated.resources.setting_dynamic_colors_description
import campfire.features.settings.ui.generated.resources.setting_dynamic_colors_title
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
    ThemeModeSetting(
      themeMode = state.theme,
      onThemeChange = { state.eventSink(SettingsUiEvent.Theme(it)) },
    )

    if (currentPlatform == Platform.ANDROID) {
      SwitchSetting(
        value = state.useDynamicColors,
        onValueChange = { state.eventSink(SettingsUiEvent.UseDynamicColors(it)) },
        headlineContent = { Text(stringResource(Res.string.setting_dynamic_colors_title)) },
        supportingContent = { Text(stringResource(Res.string.setting_dynamic_colors_description)) },
      )
    }
  }
}
