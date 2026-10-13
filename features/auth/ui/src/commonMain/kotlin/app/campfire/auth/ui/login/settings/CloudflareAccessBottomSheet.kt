// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.login.cloudflareClientId
import app.campfire.auth.ui.login.cloudflareClientSecret
import app.campfire.auth.ui.login.withCloudflareServiceToken
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Save
import app.campfire.common.compose.icons.rounded.Visibility
import app.campfire.common.compose.icons.rounded.VisibilityOff
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.model.NetworkSettings
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_hide_client_secret
import campfire.features.auth.ui.generated.resources.action_save_service_token
import campfire.features.auth.ui.generated.resources.action_show_client_secret
import campfire.features.auth.ui.generated.resources.cloudflare_access_description
import campfire.features.auth.ui.generated.resources.cloudflare_access_title
import campfire.features.auth.ui.generated.resources.label_access_client_id
import campfire.features.auth.ui.generated.resources.label_access_client_secret
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for a Cloudflare Access service token's Client ID and Client Secret, returning
 * [currentSettings] with the token's headers set. Other headers are kept.
 */
suspend fun OverlayHost.showCloudflareAccessBottomSheet(
  currentSettings: NetworkSettings?,
): NetworkSettingsResult {
  return show(
    BottomSheetOverlay<NetworkSettingsModel, NetworkSettingsResult>(
      model = NetworkSettingsModel(currentSettings),
      onDismiss = { NetworkSettingsResult.None },
      skipPartiallyExpandedState = true,
    ) { model, navigator ->
      CloudflareAccessBottomSheet(
        currentSettings = model.settings,
        onDismiss = { navigator.finish(it) },
      )
    },
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CloudflareAccessBottomSheet(
  currentSettings: NetworkSettings?,
  onDismiss: (NetworkSettingsResult) -> Unit,
  modifier: Modifier = Modifier,
) {
  val clientId = rememberTextFieldState(currentSettings.cloudflareClientId.orEmpty())
  val clientSecret = rememberTextFieldState(currentSettings.cloudflareClientSecret.orEmpty())
  var showSecret by remember { mutableStateOf(false) }

  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp),
  ) {
    Text(
      text = stringResource(Res.string.cloudflare_access_title),
      style = MaterialTheme.typography.titleMedium,
    )

    Text(
      text = stringResource(Res.string.cloudflare_access_description),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.size(8.dp))

    OutlinedTextField(
      state = clientId,
      label = { Text(stringResource(Res.string.label_access_client_id)) },
      lineLimits = TextFieldLineLimits.SingleLine,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Ascii,
        autoCorrectEnabled = false,
      ),
      modifier = Modifier.fillMaxWidth(),
    )

    OutlinedSecureTextField(
      state = clientSecret,
      label = { Text(stringResource(Res.string.label_access_client_secret)) },
      textObfuscationMode = if (showSecret) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
      trailingIcon = {
        val toggleLabel = stringResource(
          if (showSecret) Res.string.action_hide_client_secret else Res.string.action_show_client_secret,
        )
        IconButtonTooltip(text = toggleLabel) {
          IconButton(onClick = { showSecret = !showSecret }) {
            Icon(
              if (showSecret) CampfireIcons.Rounded.Visibility else CampfireIcons.Rounded.VisibilityOff,
              contentDescription = toggleLabel,
            )
          }
        }
      },
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.size(8.dp))

    val saveButtonSize = ButtonDefaults.MinHeight
    Button(
      enabled = clientId.text.isNotBlank() && clientSecret.text.isNotBlank(),
      onClick = {
        onDismiss(
          NetworkSettingsResult.Success(
            currentSettings.withCloudflareServiceToken(
              clientId = clientId.text.toString(),
              clientSecret = clientSecret.text.toString(),
            ),
          ),
        )
      },
      shapes = ButtonDefaults.shapes(),
      contentPadding = ButtonDefaults.contentPaddingFor(saveButtonSize),
      modifier = Modifier
        .heightIn(saveButtonSize)
        .align(Alignment.End),
    ) {
      Icon(
        CampfireIcons.Rounded.Save,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(saveButtonSize)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(saveButtonSize)))
      Text(
        text = stringResource(Res.string.action_save_service_token),
        style = ButtonDefaults.textStyleFor(saveButtonSize),
      )
    }

    Spacer(Modifier.size(16.dp))
  }
}

@Preview
@Composable
private fun CloudflareAccessBottomSheetPreview() {
  CampfireTheme {
    ModalBottomSheet(
      onDismissRequest = {},
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      CloudflareAccessBottomSheet(
        currentSettings = NetworkSettings(
          mapOf("CF-Access-Client-Id" to "0123456789abcdef.access"),
        ),
        onDismiss = {},
      )
    }
  }
}
