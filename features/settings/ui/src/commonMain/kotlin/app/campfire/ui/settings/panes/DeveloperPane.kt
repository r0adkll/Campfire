// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.Platform
import app.campfire.core.currentPlatform
import app.campfire.ui.settings.SettingsUiEvent.DeveloperSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.DurationInputSetting
import app.campfire.ui.settings.composables.Header
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
    Header(
      title = { Text("Account") },
    )

    ActionSetting(
      headlineContent = { Text("Invalidate current account") },
      supportingContent = {
        Text("Simulate an expired auth token on the current account for testing re-authentication.")
      },
      onClick = {
        state.eventSink(DeveloperSettingEvent.InvalidateCurrentAccount)
      },
    )

    Header(
      title = { Text("Misc") },
    )

    DurationInputSetting(
      value = state.developerSettings.sessionAge,
      onValueChange = { state.eventSink(DeveloperSettingEvent.SessionAge(it)) },
      headlineContent = { Text(stringResource(Res.string.developer_settings_session_age_title)) },
      supportingContent = { Text(stringResource(Res.string.developer_settings_session_age_subtitle)) },
    )

    if (currentPlatform == Platform.ANDROID) {
      DurationInputSetting(
        value = state.developerSettings.hlsLargeItemThreshold,
        onValueChange = { state.eventSink(DeveloperSettingEvent.HlsLargeItemThreshold(it)) },
        headlineContent = { Text("HLS large-item threshold") },
        supportingContent = {
          Text(
            "Single-file books longer than this stream over HLS when the streaming method " +
              "is set to Auto.",
          )
        },
      )

      SwitchSetting(
        value = !state.developerSettings.showWidgetPinningPrompt,
        onValueChange = {
          state.eventSink(DeveloperSettingEvent.ShowWidgetPinningChange(!it))
        },
        headlineContent = { Text("Show widget pinning dialog") },
        supportingContent = {
          Text("Next time content is played, the user will be prompted to pin the playback widget")
        },
      )

      Header(
        title = { Text("Media Buttons") },
      )

      val packages = state.developerSettings.mediaButtonPackages
      ActionSetting(
        headlineContent = { Text("Observed media button packages") },
        supportingContent = {
          Text(
            text = if (packages.isEmpty()) {
              "No skip-next / skip-previous events have been received yet. Trigger your Bluetooth " +
                "or remote control to see which package sends the events."
            } else {
              packages.sorted().joinToString(separator = "\n")
            },
          )
        },
      )

      ActionSetting(
        headlineContent = { Text("Clear observed media button packages") },
        supportingContent = { Text("Reset the list above.") },
        onClick = {
          state.eventSink(DeveloperSettingEvent.ClearMediaButtonPackages)
        },
      )
    }

    if (state.applicationInfo.debugBuild) {
      Header(
        title = { Text("App Updates") },
      )

      SwitchSetting(
        value = state.developerSettings.fakeAppUpdateSignedIn,
        onValueChange = {
          state.eventSink(DeveloperSettingEvent.FakeAppUpdateSignedIn(it))
        },
        headlineContent = { Text("Signed in for updates") },
        supportingContent = {
          Text("Fake the tester sign-in state. Turn off to test the sign-in widget in the drawer.")
        },
      )

      SwitchSetting(
        value = state.developerSettings.fakeAppUpdateAvailable,
        onValueChange = {
          state.eventSink(DeveloperSettingEvent.FakeAppUpdateAvailable(it))
        },
        headlineContent = { Text("Update available") },
        supportingContent = {
          Text("Fake an available app update to test the drawer widget and update details sheet.")
        },
      )

      SwitchSetting(
        value = state.developerSettings.fakeAppUpdateFailDownload,
        onValueChange = {
          state.eventSink(DeveloperSettingEvent.FakeAppUpdateFailDownload(it))
        },
        headlineContent = { Text("Fail update downloads") },
        supportingContent = {
          Text("Simulated update downloads will fail partway through to test the retry UX.")
        },
      )

      ActionSetting(
        headlineContent = { Text("Reset dismissed widgets") },
        supportingContent = {
          Text("Clears dismissals of the update widgets so they show in the drawer again.")
        },
        onClick = {
          state.eventSink(DeveloperSettingEvent.ResetAppUpdateDismissals)
        },
      )
    }

    Header(
      title = { Text("Analytics") },
    )

    ActionSetting(
      headlineContent = { Text("Analytics Debug State") },
      supportingContent = { Text(state.developerSettings.analyticsDebugState) },
    )
  }
}
