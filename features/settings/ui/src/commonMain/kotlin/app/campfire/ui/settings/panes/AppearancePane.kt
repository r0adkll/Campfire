package app.campfire.ui.settings.panes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.campfire.core.Platform
import app.campfire.core.currentPlatform
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.DropdownSetting
import app.campfire.ui.settings.composables.Header
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

    Header(
      title = { Text("Overall") }
    )

    ThemeModeSetting(
      themeMode = state.theme,
      onThemeChange = { state.eventSink(SettingsUiEvent.AppearanceSettingEvent.Theme(it)) },
    )

    if (currentPlatform == Platform.ANDROID) {
      SwitchSetting(
        value = state.useDynamicColors,
        onValueChange = { state.eventSink(SettingsUiEvent.AppearanceSettingEvent.UseDynamicColors(it)) },
        headlineContent = { Text(stringResource(Res.string.setting_dynamic_colors_title)) },
        supportingContent = { Text(stringResource(Res.string.setting_dynamic_colors_description)) },
      )
    }

    Header(
      title = { Text("Dynamic") }
    )

    SwitchSetting(
      value = true,
      onValueChange = {},
      headlineContent = { Text("Item detail") },
      supportingContent = { Text("Theme the item detail screen based on the item's thumbnail image") },
    )

    SwitchSetting(
      value = true,
      onValueChange = {},
      headlineContent = { Text("Playback UI") },
      supportingContent = { Text("Theme the player view based on the item's thumbnail image") },
    )

    ActionSetting(
      headlineContent = { Text("Swatch color selector") },
      supportingContent = { Text("The color choice from the analyzed item cover image") },
      trailingContent = {
        Text(
          text = "Dominant",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.Bold,
        )
      },
      onClick =  {
        // TODO: Show swatch picker editor
      }
    )

    ActionSetting(
      headlineContent = { Text("Dynamic color scheme") },
      supportingContent = { Text("The type of color scheme to use when generating based on content") },
      trailingContent = {
        Text(
          text = "Expressive",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.Bold,
        )
      },
      onClick =  {
        // TODO: Show swatch picker editor
      }
    )
  }
}
