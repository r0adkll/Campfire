package app.campfire.ui.settings.panes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    }

    Header(
      title = { Text("Analytics") },
    )

    ActionSetting(
      headlineContent = { Text("Analytics Debug State") },
      supportingContent = { Text(state.developerSettings.analyticsDebugState) },
    )

    if (currentPlatform == Platform.ANDROID && !state.developerSettings.isAndroidAutoAvailable) {
      Header(
        title = { Text("Android Auto") },
      )

      ElevatedCard(
        modifier = Modifier
          .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
          ),
        colors = CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
      ) {
        Row(
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(48.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.Warning, contentDescription = null)
          Text(
            text = "Warning",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Text(
          text = buildAnnotatedString {
            appendLine(
              "This version of the app was installed from an \"Unknown Source\" (i.e. not the Google PlayStore) " +
                "and will therefore not be visible in Android Auto by default. To enable it and access " +
                "Android Auto functionality, you must do the following:",
            )

            appendBulletedList(
              "Open the Android Auto settings (see below)",
              "Scroll down to 'Version' and click it until Developer mode is enabled",
              "Click the '⋮' icon, and then 'Developer settings'",
              "Enable 'Unknown sources'",
              "Re-connect Android Auto!",
            )
          },
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
          ),
        )
      }

      ActionSetting(
        headlineContent = { Text("Open Android Auto settings") },
        leadingContent = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
        onClick = { state.eventSink(DeveloperSettingEvent.OpenAndroidAutoSettings) },
      )
    }
  }
}

fun AnnotatedString.Builder.appendBulletedList(vararg items: String) {
  val bulletParagraphStyle = ParagraphStyle(textIndent = TextIndent(restLine = 16.sp))
  items.forEach {
    withStyle(bulletParagraphStyle) {
      append("\u2022")
      append("\t\t")
      append(it)
    }
  }
}
