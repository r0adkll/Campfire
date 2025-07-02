package app.campfire.ui.settings.panes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.ui.settings.SettingsUiEvent.DeveloperSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.DurationInputSetting
import app.campfire.ui.settings.composables.SwitchSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.developer_settings_session_age_subtitle
import campfire.features.settings.ui.generated.resources.developer_settings_session_age_title
import campfire.features.settings.ui.generated.resources.developer_settings_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeveloperPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.developer_settings_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    DurationInputSetting(
      value = state.developerSettings.sessionAge,
      onValueChange = { state.eventSink(DeveloperSettingEvent.SessionAge(it)) },
      headlineContent = { Text(stringResource(Res.string.developer_settings_session_age_title)) },
      supportingContent = { Text(stringResource(Res.string.developer_settings_session_age_subtitle)) },
    )

    SwitchSetting(
      value = !state.developerSettings.showWidgetPinningPrompt,
      onValueChange = {
        state.eventSink(DeveloperSettingEvent.ShowWidgetPinningChange(!it))
      },
      headlineContent = { Text("Show widget pinning dialog") },
      supportingContent = { Text("Next time content is played, the user will be prompted to pin the playback widget") },
    )
  }
}
