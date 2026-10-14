// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.connect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.auth.api.screen.ConnectScreen
import app.campfire.auth.ui.composables.MaxContentWidth
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CheckCircle
import app.campfire.common.compose.icons.rounded.Error
import app.campfire.common.compose.icons.rounded.NewReleases
import app.campfire.common.compose.icons.rounded.Policy
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeImage
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_cancel
import campfire.features.auth.ui.generated.resources.action_close
import campfire.features.auth.ui.generated.resources.action_connect_add
import campfire.features.auth.ui.generated.resources.action_connect_update
import campfire.features.auth.ui.generated.resources.action_done
import campfire.features.auth.ui.generated.resources.connect_invalid_message
import campfire.features.auth.ui.generated.resources.connect_invalid_title
import campfire.features.auth.ui.generated.resources.connect_label_server
import campfire.features.auth.ui.generated.resources.connect_step_openid
import campfire.features.auth.ui.generated.resources.connect_step_save
import campfire.features.auth.ui.generated.resources.connect_step_sign_in
import campfire.features.auth.ui.generated.resources.connect_steps_title
import campfire.features.auth.ui.generated.resources.connect_title_new
import campfire.features.auth.ui.generated.resources.connect_title_update
import campfire.features.auth.ui.generated.resources.connect_token_notice
import campfire.features.auth.ui.generated.resources.connect_trust_title
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
  Surface(modifier = modifier.fillMaxSize()) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.systemBarsPadding(),
    ) {
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
          isError = true,
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
  val isNew = content.existingUserName == null
  ConnectLayout(
    modifier = modifier,
    actions = {
      Button(
        onClick = onConfirm,
        enabled = !content.isSaving,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = ActionHeight),
      ) {
        if (content.isSaving) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = LocalContentColor.current,
            modifier = Modifier.size(20.dp),
          )
        } else {
          Text(
            text = stringResource(if (isNew) Res.string.action_connect_add else Res.string.action_connect_update),
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }
      TextButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(Res.string.action_cancel))
      }
    },
  ) {
    // The same tent a new campsite starts with on the sign-in screen
    HeroCircle {
      AppThemeImage(appTheme = AppTheme.Fixed.Tent, modifier = Modifier.size(52.dp))
    }
    Text(
      text = stringResource(if (isNew) Res.string.connect_title_new else Res.string.connect_title_update),
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )

    // The host is what the token goes to, so it leads; the name is whatever the link says
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      shape = MaterialTheme.shapes.large,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(20.dp),
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

    TrustCallout(
      message = if (isNew) {
        stringResource(Res.string.connect_token_notice, content.serverHost)
      } else {
        stringResource(Res.string.connect_update_notice, content.existingUserName.orEmpty(), content.serverHost)
      },
    )

    if (isNew) {
      NextSteps(
        steps = listOf(
          stringResource(Res.string.connect_step_save),
          stringResource(
            if (content.signInWithOpenId) Res.string.connect_step_openid else Res.string.connect_step_sign_in,
          ),
        ),
      )
    }
  }
}

@Composable
private fun TrustCallout(
  message: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    shape = MaterialTheme.shapes.large,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.padding(20.dp),
    ) {
      Icon(CampfireIcons.Rounded.Policy, contentDescription = null)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(Res.string.connect_trust_title),
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable
private fun NextSteps(
  steps: List<String>,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      text = stringResource(Res.string.connect_steps_title),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    steps.forEachIndexed { index, step ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = Color.Transparent,
          contentColor = MaterialTheme.colorScheme.primary,
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
          modifier = Modifier.size(28.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = (index + 1).toString(),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
        Text(
          text = step,
          style = MaterialTheme.typography.bodyLarge,
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
  isError: Boolean = false,
) {
  ConnectLayout(
    modifier = modifier,
    actions = {
      Button(
        onClick = onAction,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = ActionHeight),
      ) {
        Text(action, style = MaterialTheme.typography.titleMedium)
      }
    },
  ) {
    HeroIcon(icon, isError = isError)
    Text(
      text = title,
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun HeroIcon(
  icon: ImageVector,
  modifier: Modifier = Modifier,
  isError: Boolean = false,
) {
  HeroCircle(modifier = modifier, isError = isError) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
  }
}

@Composable
private fun HeroCircle(
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  content: @Composable () -> Unit,
) {
  Surface(
    shape = CircleShape,
    color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
    contentColor = if (isError) {
      MaterialTheme.colorScheme.onErrorContainer
    } else {
      MaterialTheme.colorScheme.onPrimaryContainer
    },
    modifier = modifier.size(80.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      content()
    }
  }
}

/**
 * Scrolling [content], centered vertically while it fits, with [actions] pinned to the bottom,
 * all within the auth screens' content width.
 */
@Composable
private fun ConnectLayout(
  actions: @Composable ColumnScope.() -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .fillMaxSize()
      .widthIn(max = MaxContentWidth),
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
    ) {
      // At least as tall as the space, so short content sits centered
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .heightIn(min = maxHeight)
          .padding(horizontal = 24.dp, vertical = 32.dp),
        content = content,
      )
    }
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 16.dp),
      content = actions,
    )
  }
}

private val ActionHeight = 56.dp
