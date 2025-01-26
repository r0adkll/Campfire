package app.campfire.ui.settings.panes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Github
import app.campfire.common.compose.icons.rounded.Policy
import app.campfire.common.compose.icons.rounded.ShieldPerson
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.AttributionsClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.DeveloperClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.GithubClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.PrivacyPolicyClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.TermsOfServiceClick
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.about_attributions_title
import campfire.features.settings.ui.generated.resources.about_developer_oss_subtitle
import campfire.features.settings.ui.generated.resources.about_developer_oss_title
import campfire.features.settings.ui.generated.resources.about_developer_subtitle
import campfire.features.settings.ui.generated.resources.about_developer_title
import campfire.features.settings.ui.generated.resources.about_header_developer
import campfire.features.settings.ui.generated.resources.about_header_legal
import campfire.features.settings.ui.generated.resources.about_privacy_policy_title
import campfire.features.settings.ui.generated.resources.about_tos_title
import campfire.features.settings.ui.generated.resources.about_version_title
import campfire.features.settings.ui.generated.resources.setting_about_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AboutPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_about_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    fun sendEvent(event: SettingsUiEvent.AboutSettingEvent) {
      state.eventSink(event)
    }

    Header(
      title = { Text(stringResource(Res.string.about_header_developer)) },
    )

    ActionSetting(
      leadingContent = { Icon(Icons.Rounded.ShieldPerson, contentDescription = null) },
      headlineContent = { Text(stringResource(Res.string.about_developer_title)) },
      supportingContent = { Text(stringResource(Res.string.about_developer_subtitle)) },
      onClick = { sendEvent(DeveloperClick) },
    )

    ActionSetting(
      leadingContent = { Icon(CampfireIcons.Rounded.Github, contentDescription = null) },
      headlineContent = { Text(stringResource(Res.string.about_developer_oss_title)) },
      supportingContent = { Text(stringResource(Res.string.about_developer_oss_subtitle)) },
      onClick = { sendEvent(GithubClick) },
    )

    Header(
      title = { Text(stringResource(Res.string.about_header_legal)) },
    )

    ActionSetting(
      leadingContent = { Icon(Icons.Rounded.Policy, contentDescription = null) },
      headlineContent = { Text(stringResource(Res.string.about_privacy_policy_title)) },
      onClick = { sendEvent(PrivacyPolicyClick) },
    )

    ActionSetting(
      leadingContent = { Icon(Icons.Rounded.Copyright, contentDescription = null) },
      headlineContent = { Text(stringResource(Res.string.about_tos_title)) },
      onClick = { sendEvent(TermsOfServiceClick) },
    )

    ActionSetting(
      leadingContent = { Icon(Icons.Rounded.Code, contentDescription = null) },
      headlineContent = { Text(stringResource(Res.string.about_attributions_title)) },
      onClick = { sendEvent(AttributionsClick) },
    )

    ActionSetting(
      headlineContent = { Text(stringResource(Res.string.about_version_title)) },
      supportingContent = { Text(state.applicationInfo.settingsReadableVersionName) },
    )
  }
}

private val ApplicationInfo.settingsReadableVersionName: String get() {
  return buildString {
    append(versionName).append(".")
    append(versionCode.toString())
    if (flavor != Flavor.Standard) {
      append("-${flavor.name}")
    }
  }
}
