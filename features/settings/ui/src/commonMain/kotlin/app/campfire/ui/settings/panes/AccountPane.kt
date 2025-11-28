package app.campfire.ui.settings.panes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.coroutines.onLoaded
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.TextFieldSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_account_dialog_label
import campfire.features.settings.ui.generated.resources.setting_account_dialog_title
import campfire.features.settings.ui.generated.resources.setting_account_logout
import campfire.features.settings.ui.generated.resources.setting_account_name_subtitle
import campfire.features.settings.ui.generated.resources.setting_account_server_appearance_title
import campfire.features.settings.ui.generated.resources.setting_account_server_title
import campfire.features.settings.ui.generated.resources.setting_account_server_url
import campfire.features.settings.ui.generated.resources.setting_account_server_version
import campfire.features.settings.ui.generated.resources.setting_account_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AccountPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_account_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    state.server.onLoaded { server ->
      Header(
        title = { Text(stringResource(Res.string.setting_account_server_appearance_title)) },
      )

//      TentSetting(
//        tent = server.tent,
//        onTentChange = { state.eventSink(SettingsUiEvent.AccountSettingEvent.ChangeTent(it)) },
//      )

      TextFieldSetting(
        value = server.name,
        onValueChange = { state.eventSink(SettingsUiEvent.AccountSettingEvent.ChangeName(it)) },
        supportingContent = { Text(stringResource(Res.string.setting_account_name_subtitle)) },
        dialogIcon = {
          Icon(Icons.Rounded.Dns, contentDescription = null)
        },
        dialogTitle = { Text(stringResource(Res.string.setting_account_dialog_title)) },
        dialogInputLabel = { Text(stringResource(Res.string.setting_account_dialog_label)) },
      )

      Header(
        title = { Text(stringResource(Res.string.setting_account_server_title)) },
      )

      ActionSetting(
        headlineContent = { Text(stringResource(Res.string.setting_account_server_url)) },
        supportingContent = { Text(server.url) },
      )

      ActionSetting(
        headlineContent = { Text(stringResource(Res.string.setting_account_server_version)) },
        supportingContent = { Text(server.settings.version) },
      )
    }

    ActionSetting(
      headlineContent = { Text(stringResource(Res.string.setting_account_logout)) },
      trailingContent = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
      onClick = {
        state.eventSink(SettingsUiEvent.AccountSettingEvent.Logout)
      },
    )
  }
}
