// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.NoPhotography
import campfire.infra.qrscanner.generated.resources.Res
import campfire.infra.qrscanner.generated.resources.action_allow_camera
import campfire.infra.qrscanner.generated.resources.action_open_app_settings
import campfire.infra.qrscanner.generated.resources.camera_permission_denied_message
import campfire.infra.qrscanner.generated.resources.camera_permission_message
import campfire.infra.qrscanner.generated.resources.camera_permission_title
import campfire.infra.qrscanner.generated.resources.camera_unavailable_message
import campfire.infra.qrscanner.generated.resources.camera_unavailable_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for the camera. Once Android stops asking for it, [denied] sends the user to Campfire's
 * app settings instead, through [onAction].
 */
@Composable
internal fun CameraPermissionMessage(
  denied: Boolean,
  onAction: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CameraMessage(
    icon = CampfireIcons.Rounded.NoPhotography,
    title = stringResource(Res.string.camera_permission_title),
    message = stringResource(
      if (denied) Res.string.camera_permission_denied_message else Res.string.camera_permission_message,
    ),
    action = stringResource(if (denied) Res.string.action_open_app_settings else Res.string.action_allow_camera),
    onAction = onAction,
    modifier = modifier,
  )
}

@Composable
internal fun CameraUnavailableMessage(modifier: Modifier = Modifier) {
  CameraMessage(
    icon = CampfireIcons.Rounded.NoPhotography,
    title = stringResource(Res.string.camera_unavailable_title),
    message = stringResource(Res.string.camera_unavailable_message),
    modifier = modifier,
  )
}

/**
 * Stands in for the camera's view, so it's light on dark like the rest of the viewfinder, whatever
 * the app's theme.
 */
@Composable
private fun CameraMessage(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  action: String? = null,
  onAction: () -> Unit = {},
) {
  Box(modifier, contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .widthIn(max = 360.dp)
        .padding(24.dp),
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = Color.White.copy(alpha = SecondaryAlpha),
        modifier = Modifier.size(48.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = SecondaryAlpha),
        textAlign = TextAlign.Center,
      )
      if (action != null) {
        Button(
          onClick = onAction,
          colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
          modifier = Modifier.padding(top = 8.dp),
        ) {
          Text(action)
        }
      }
    }
  }
}

private const val SecondaryAlpha = 0.72f
