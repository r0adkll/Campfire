// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Add
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.Password
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.ui.settings.HeaderError
import app.campfire.ui.settings.SettingsUiEvent.ConnectionSettingEvent
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.SettingListItem
import app.campfire.ui.settings.validateHeader
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.dialog_confirm_action
import campfire.features.settings.ui.generated.resources.dialog_dismiss_action
import campfire.features.settings.ui.generated.resources.setting_connection_headers_add
import campfire.features.settings.ui.generated.resources.setting_connection_headers_description
import campfire.features.settings.ui.generated.resources.setting_connection_headers_dialog_add_title
import campfire.features.settings.ui.generated.resources.setting_connection_headers_dialog_edit_title
import campfire.features.settings.ui.generated.resources.setting_connection_headers_empty
import campfire.features.settings.ui.generated.resources.setting_connection_headers_error_invalid_name
import campfire.features.settings.ui.generated.resources.setting_connection_headers_error_invalid_value
import campfire.features.settings.ui.generated.resources.setting_connection_headers_error_missing_name
import campfire.features.settings.ui.generated.resources.setting_connection_headers_error_missing_value
import campfire.features.settings.ui.generated.resources.setting_connection_headers_header
import campfire.features.settings.ui.generated.resources.setting_connection_headers_name_label
import campfire.features.settings.ui.generated.resources.setting_connection_headers_remove
import campfire.features.settings.ui.generated.resources.setting_connection_headers_value_label
import org.jetbrains.compose.resources.stringResource

/**
 * The extra headers sent with every request (and the socket handshake). Values are often secrets
 * (proxy tokens), so they're masked in the list and in the editor.
 */
@Composable
internal fun CustomHeaderSettings(
  headers: Map<String, String>,
  onEvent: (ConnectionSettingEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  // null = closed; "" = adding; a name = editing that header
  var editing by remember { mutableStateOf<String?>(null) }

  Column(modifier) {
    Header(
      title = { Text(stringResource(Res.string.setting_connection_headers_header)) },
    )

    SettingListItem(
      headlineContent = { Text(stringResource(Res.string.setting_connection_headers_description)) },
    )

    if (headers.isEmpty()) {
      SettingListItem(
        headlineContent = { Text(stringResource(Res.string.setting_connection_headers_empty)) },
      )
    }

    headers.keys.sortedWith(String.CASE_INSENSITIVE_ORDER).forEach { name ->
      SettingListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(MASKED_VALUE) },
        leadingContent = { Icon(CampfireIcons.Rounded.Password, contentDescription = null) },
        trailingContent = {
          val removeLabel = stringResource(Res.string.setting_connection_headers_remove)
          IconButtonTooltip(text = removeLabel) {
            IconButton(onClick = { onEvent(ConnectionSettingEvent.RemoveHeader(name)) }) {
              Icon(CampfireIcons.Rounded.Delete, contentDescription = removeLabel)
            }
          }
        },
        modifier = Modifier.clickable { editing = name },
      )
    }

    ActionSetting(
      headlineContent = { Text(stringResource(Res.string.setting_connection_headers_add)) },
      leadingContent = { Icon(CampfireIcons.Rounded.Add, contentDescription = null) },
      onClick = { editing = "" },
    )
  }

  editing?.let { original ->
    HeaderDialog(
      originalName = original.ifEmpty { null },
      originalValue = headers[original].orEmpty(),
      onSave = { name, value ->
        onEvent(ConnectionSettingEvent.SaveHeader(original.ifEmpty { null }, name, value))
        editing = null
      },
      onDismiss = { editing = null },
    )
  }
}

@Composable
private fun HeaderDialog(
  originalName: String?,
  originalValue: String,
  onSave: (name: String, value: String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var name by remember { mutableStateOf(originalName.orEmpty()) }
  var value by remember { mutableStateOf(originalValue) }
  var showErrors by remember { mutableStateOf(false) }
  val error = validateHeader(name, value)

  val save = {
    if (error == null) onSave(name.trim(), value.trim()) else showErrors = true
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(CampfireIcons.Rounded.Password, contentDescription = null) },
    title = {
      Text(
        stringResource(
          if (originalName == null) {
            Res.string.setting_connection_headers_dialog_add_title
          } else {
            Res.string.setting_connection_headers_dialog_edit_title
          },
        ),
      )
    },
    text = {
      Column {
        TextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(Res.string.setting_connection_headers_name_label)) },
          singleLine = true,
          isError = showErrors && (error == HeaderError.MissingName || error == HeaderError.InvalidName),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        TextField(
          value = value,
          onValueChange = { value = it },
          label = { Text(stringResource(Res.string.setting_connection_headers_value_label)) },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          isError = showErrors && (error == HeaderError.MissingValue || error == HeaderError.InvalidValue),
          supportingText = if (showErrors && error != null) {
            { Text(error.message()) }
          } else {
            null
          },
          // The keyboard can cover the dialog buttons (e.g. in landscape), so Done saves too
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { save() }),
        )
      }
    },
    confirmButton = {
      TextButton(onClick = save) {
        Text(stringResource(Res.string.dialog_confirm_action))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.dialog_dismiss_action))
      }
    },
    modifier = modifier,
  )
}

@Composable
private fun HeaderError.message(): String = stringResource(
  when (this) {
    HeaderError.MissingName -> Res.string.setting_connection_headers_error_missing_name
    HeaderError.InvalidName -> Res.string.setting_connection_headers_error_invalid_name
    HeaderError.MissingValue -> Res.string.setting_connection_headers_error_missing_value
    HeaderError.InvalidValue -> Res.string.setting_connection_headers_error_invalid_value
  },
)

private const val MASKED_VALUE = "••••••••"
