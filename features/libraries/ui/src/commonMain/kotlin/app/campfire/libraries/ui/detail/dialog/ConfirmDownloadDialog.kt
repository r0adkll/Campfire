package app.campfire.libraries.ui.detail.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.LibraryItem
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.dialog_download_action_confirm
import campfire.features.libraries.ui.generated.resources.dialog_download_action_dismiss
import campfire.features.libraries.ui.generated.resources.dialog_download_do_not_show_label
import campfire.features.libraries.ui.generated.resources.dialog_download_message_prefix
import campfire.features.libraries.ui.generated.resources.dialog_download_message_suffix
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConfirmDownloadDialog(
  item: LibraryItem,
  onConfirm: (doNotShowAgain: Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var doNotShowAgain by remember { mutableStateOf(false) }
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = {
      Text(
        buildAnnotatedString {
          append("Download ")
          withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append("\"${item.media.metadata.title}\"")
          }
        },
      )
    },
    text = {
      Column {
        Text(
          buildAnnotatedString {
            append(stringResource(Res.string.dialog_download_message_prefix))
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
              append(item.media.sizeInBytes.asReadableBytes())
            }
            append(" ")
            append(stringResource(Res.string.dialog_download_message_suffix))
          },
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(Res.string.dialog_download_do_not_show_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
          )
          Switch(
            checked = doNotShowAgain,
            onCheckedChange = { doNotShowAgain = it },
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onConfirm(doNotShowAgain)
        },
      ) {
        Text(stringResource(Res.string.dialog_download_action_confirm))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismissRequest,
      ) {
        Text(stringResource(Res.string.dialog_download_action_dismiss))
      }
    },
  )
}
