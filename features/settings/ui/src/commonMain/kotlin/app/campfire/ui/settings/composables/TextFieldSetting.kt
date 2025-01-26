package app.campfire.ui.settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.dialog_confirm_action
import campfire.features.settings.ui.generated.resources.dialog_dismiss_action
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TextFieldSetting(
  value: String,
  onValueChange: (String) -> Unit,
  supportingContent: @Composable () -> Unit,
  dialogIcon: (@Composable () -> Unit)? = null,
  dialogTitle: @Composable () -> Unit,
  dialogInputLabel: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDialog by remember { mutableStateOf(false) }

  SettingListItem(
    headlineContent = { Text(value) },
    supportingContent = supportingContent,
    modifier = modifier
      .clickable {
        showDialog = true
      },
  )

  if (showDialog) {
    var text by remember {
      mutableStateOf(
        TextFieldValue(
          text = value,
          selection = TextRange(0, value.length),
        ),
      )
    }

    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = dialogTitle,
      icon = dialogIcon,
      text = {
        TextField(
          value = text,
          onValueChange = { text = it },
          label = dialogInputLabel,
          singleLine = true,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onValueChange(text.text)
            showDialog = false
          },
        ) {
          Text(stringResource(Res.string.dialog_confirm_action))
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDialog = false },
        ) {
          Text(stringResource(Res.string.dialog_dismiss_action))
        }
      },
    )
  }
}
