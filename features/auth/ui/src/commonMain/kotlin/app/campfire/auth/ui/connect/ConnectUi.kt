// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.auth.api.screen.ConnectScreen
import app.campfire.auth.ui.composables.MaxContentWidth
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CheckCircle
import app.campfire.common.compose.icons.rounded.Dns
import app.campfire.common.compose.icons.rounded.Error
import app.campfire.common.compose.icons.rounded.NewReleases
import app.campfire.common.compose.icons.rounded.Policy
import app.campfire.core.di.UserScope
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_cancel
import campfire.features.auth.ui.generated.resources.action_close
import campfire.features.auth.ui.generated.resources.action_connect_add
import campfire.features.auth.ui.generated.resources.action_connect_update
import campfire.features.auth.ui.generated.resources.action_done
import campfire.features.auth.ui.generated.resources.connect_invalid_message
import campfire.features.auth.ui.generated.resources.connect_invalid_title
import campfire.features.auth.ui.generated.resources.connect_label_server
import campfire.features.auth.ui.generated.resources.connect_next_openid
import campfire.features.auth.ui.generated.resources.connect_next_sign_in
import campfire.features.auth.ui.generated.resources.connect_title_new
import campfire.features.auth.ui.generated.resources.connect_title_update
import campfire.features.auth.ui.generated.resources.connect_token_notice
import campfire.features.auth.ui.generated.resources.connect_unsupported_message
import campfire.features.auth.ui.generated.resources.connect_unsupported_title
import campfire.features.auth.ui.generated.resources.connect_update_notice
import campfire.features.auth.ui.generated.resources.connect_updated_message
import campfire.features.auth.ui.generated.resources.connect_updated_title
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(ConnectScreen::class, UserScope::class)
@Composable
fun Connect(
  state: ConnectUiState,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxSize()
      .systemBarsPadding(),
  ) {
    Box(contentAlignment = Alignment.Center) {
      when (val content = state.content) {
        ConnectContent.Loading -> CircularProgressIndicator()

        is ConnectContent.Confirm -> ConfirmConnect(
          content = content,
          onConfirm = { state.eventSink(ConnectUiEvent.Confirm) },
          onCancel = { state.eventSink(ConnectUiEvent.Dismiss) },
        )

        is ConnectContent.Updated -> ConnectMessage(
          icon = CampfireIcons.Rounded.CheckCircle,
          title = stringResource(Res.string.connect_updated_title),
          message = stringResource(Res.string.connect_updated_message, content.userName, content.serverHost),
          action = stringResource(Res.string.action_done),
          onAction = { state.eventSink(ConnectUiEvent.Dismiss) },
        )

        ConnectContent.UnsupportedVersion -> ConnectMessage(
          icon = CampfireIcons.Rounded.NewReleases,
          title = stringResource(Res.string.connect_unsupported_title),
          message = stringResource(Res.string.connect_unsupported_message),
          action = stringResource(Res.string.action_close),
          onAction = { state.eventSink(ConnectUiEvent.Dismiss) },
        )

        ConnectContent.Invalid -> ConnectMessage(
          icon = CampfireIcons.Rounded.Error,
          title = stringResource(Res.string.connect_invalid_title),
          message = stringResource(Res.string.connect_invalid_message),
          action = stringResource(Res.string.action_close),
          onAction = { state.eventSink(ConnectUiEvent.Dismiss) },
        )
      }
    }
  }
}

@Composable
private fun ConfirmConnect(
  content: ConnectContent.Confirm,
  onConfirm: () -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ConnectColumn(modifier) {
    Icon(
      CampfireIcons.Rounded.Dns,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(48.dp),
    )
    Text(
      text = stringResource(
        if (content.existingUserName == null) Res.string.connect_title_new else Res.string.connect_title_update,
      ),
      style = MaterialTheme.typography.headlineSmall,
    )

    // The host is what the token goes to, so it leads; the name is whatever the link says
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = stringResource(Res.string.connect_label_server),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = content.serverHost,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        content.serverName?.let { name ->
          Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(
        CampfireIcons.Rounded.Policy,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = if (content.existingUserName == null) {
          stringResource(Res.string.connect_token_notice, content.serverHost)
        } else {
          stringResource(Res.string.connect_update_notice, content.existingUserName, content.serverHost)
        },
        style = MaterialTheme.typography.bodyMedium,
      )
    }

    if (content.existingUserName == null) {
      Text(
        text = stringResource(
          if (content.signInWithOpenId) Res.string.connect_next_openid else Res.string.connect_next_sign_in,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
      TextButton(onClick = onCancel) {
        Text(stringResource(Res.string.action_cancel))
      }
      Button(
        onClick = onConfirm,
        enabled = !content.isSaving,
      ) {
        Text(
          stringResource(
            if (content.existingUserName == null) Res.string.action_connect_add else Res.string.action_connect_update,
          ),
        )
      }
    }
  }
}

@Composable
private fun ConnectMessage(
  icon: ImageVector,
  title: String,
  message: String,
  action: String,
  onAction: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ConnectColumn(modifier) {
    Icon(
      icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(48.dp),
    )
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Text(message, style = MaterialTheme.typography.bodyLarge)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
    ) {
      Button(onClick = onAction) {
        Text(action)
      }
    }
  }
}

@Composable
private fun ConnectColumn(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = modifier
      .widthIn(max = MaxContentWidth)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    content()
  }
}
