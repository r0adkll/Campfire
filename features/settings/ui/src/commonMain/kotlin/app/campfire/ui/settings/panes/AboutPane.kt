package app.campfire.ui.settings.panes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LogoDev
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.LogoDev
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AreaChart
import app.campfire.common.compose.icons.rounded.Crash
import app.campfire.common.compose.icons.rounded.Github
import app.campfire.common.compose.icons.rounded.LogoDev
import app.campfire.common.compose.icons.rounded.Policy
import app.campfire.common.compose.icons.rounded.ShieldPerson
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.toast.Toast
import app.campfire.core.toast.ToastHandle
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.AttributionsClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.ChangelogClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.DeveloperClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.GithubClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.PrivacyPolicyClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.TermsOfServiceClick
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.SwitchSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.about_attributions_title
import campfire.features.settings.ui.generated.resources.about_changelog_title
import campfire.features.settings.ui.generated.resources.about_data_analytic_reporting_subtitle
import campfire.features.settings.ui.generated.resources.about_data_analytic_reporting_title
import campfire.features.settings.ui.generated.resources.about_data_crash_reporting_subtitle
import campfire.features.settings.ui.generated.resources.about_data_crash_reporting_title
import campfire.features.settings.ui.generated.resources.about_developer_oss_subtitle
import campfire.features.settings.ui.generated.resources.about_developer_oss_title
import campfire.features.settings.ui.generated.resources.about_developer_subtitle
import campfire.features.settings.ui.generated.resources.about_developer_title
import campfire.features.settings.ui.generated.resources.about_header_data_collection
import campfire.features.settings.ui.generated.resources.about_header_developer
import campfire.features.settings.ui.generated.resources.about_header_legal
import campfire.features.settings.ui.generated.resources.about_privacy_policy_title
import campfire.features.settings.ui.generated.resources.about_tos_title
import campfire.features.settings.ui.generated.resources.about_version_title
import campfire.features.settings.ui.generated.resources.setting_about_title
import kotlinx.coroutines.delay
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
      title = { Text(stringResource(Res.string.about_header_data_collection)) },
    )

    SwitchSetting(
      value = state.aboutSettings.crashReportingEnabled,
      onValueChange = { sendEvent(SettingsUiEvent.AboutSettingEvent.CrashReportingEnabled(it)) },
      headlineContent = { Text(stringResource(Res.string.about_data_crash_reporting_title)) },
      supportingContent = { Text(stringResource(Res.string.about_data_crash_reporting_subtitle)) },
      leadingContent = { Icon(CampfireIcons.Rounded.Crash, contentDescription = null) },
    )

    SwitchSetting(
      value = state.aboutSettings.analyticReportingEnabled,
      onValueChange = { sendEvent(SettingsUiEvent.AboutSettingEvent.AnalyticReportingEnabled(it)) },
      headlineContent = { Text(stringResource(Res.string.about_data_analytic_reporting_title)) },
      supportingContent = { Text(stringResource(Res.string.about_data_analytic_reporting_subtitle)) },
      leadingContent = { Icon(CampfireIcons.Rounded.AreaChart, contentDescription = null) },
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
      leadingContent = { Icon(CampfireIcons.Rounded.LogoDev, contentDescription = null) },
      headlineContent = { Text(stringResource(Res.string.about_changelog_title)) },
      onClick = { sendEvent(ChangelogClick) },
    )

    val toast = LocalToast.current
    var versionClickCount by remember { mutableIntStateOf(0) }
    var priorToast by remember { mutableStateOf<ToastHandle?>(null) }
    LaunchedEffect(versionClickCount) {
      if (versionClickCount == 0) return@LaunchedEffect
      priorToast?.cancel()
      priorToast = null

      if (versionClickCount >= DEVELOPER_MODE_CLICKS) {
        state.eventSink(SettingsUiEvent.DeveloperSettingEvent.EnableDeveloperMode)
        versionClickCount = 0
        toast.show("Developer mode enabled!", Toast.Duration.SHORT)
        return@LaunchedEffect
      }

      val remainingClicks = DEVELOPER_MODE_CLICKS - versionClickCount
      priorToast = toast.show("$remainingClicks more clicks to enable developer mode!", Toast.Duration.SHORT)

      delay(DEVELOPER_MODE_CLICK_TIMEOUT)
      versionClickCount = 0
    }

    ActionSetting(
      headlineContent = { Text(stringResource(Res.string.about_version_title)) },
      supportingContent = { Text(state.applicationInfo.settingsReadableVersionName) },
      onClick = {
        versionClickCount++
        Unit
      }.takeIf { !state.developerSettings.developerModeEnabled },
    )
  }
}

internal const val DEVELOPER_MODE_CLICKS = 6
internal const val DEVELOPER_MODE_CLICK_TIMEOUT = 1500L

private val ApplicationInfo.settingsReadableVersionName: String get() {
  return buildString {
    append(versionName).append(" ")
    append("($versionCode)")
  }
}
