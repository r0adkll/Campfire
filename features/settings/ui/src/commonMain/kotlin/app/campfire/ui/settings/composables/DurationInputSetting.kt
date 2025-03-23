package app.campfire.ui.settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.dialog_confirm_action
import campfire.features.settings.ui.generated.resources.dialog_dismiss_action
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DurationInputSetting(
  value: Duration,
  onValueChange: (Duration) -> Unit,
  headlineContent: @Composable () -> Unit,
  supportingContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  var showDialog by remember { mutableStateOf(false) }

  SettingListItem(
    headlineContent = headlineContent,
    supportingContent = supportingContent,
    trailingContent = {
      Text(
        text = value.toString(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    },
    modifier = modifier
      .clickable {
        showDialog = true
      },
  )

  if (showDialog) {
    TimeInputPickerDialog(
      current = value,
      onConfirm = onValueChange,
      onDismissRequest = { showDialog = false },
      title = headlineContent,
    )
  }
}

@Composable
private fun TimeInputPickerDialog(
  current: Duration,
  onConfirm: (Duration) -> Unit,
  onDismissRequest: () -> Unit,
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  val initialHour = current.inWholeHours.coerceIn(0L..23L).toInt()
  val initialMinute = current.inWholeMinutes.mod(60L).coerceIn(0L..59L).toInt()

  BasicAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  ) {
    val timePickerState = rememberTimePickerState(
      initialHour = initialHour,
      initialMinute = initialMinute,
      is24Hour = true,
    )
    Surface(
      modifier = Modifier.wrapContentWidth().wrapContentHeight(),
      shape = MaterialTheme.shapes.large,
      tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
      ) {
        CompositionLocalProvider(
          LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        ) {
          ProvideTextStyle(
            MaterialTheme.typography.headlineSmall,
          ) {
            title()
          }
        }

        Spacer(Modifier.height(16.dp))

        TimeInput(
          state = timePickerState,
          modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
          TextButton(
            onClick = onDismissRequest,
          ) {
            Text(stringResource(Res.string.dialog_dismiss_action))
          }
          TextButton(
            enabled = timePickerState.hour != initialHour ||
              timePickerState.minute != initialMinute,
            onClick = {
              val updatedDuration = timePickerState.hour.hours + timePickerState.minute.minutes
              onConfirm(updatedDuration)
              onDismissRequest()
            },
          ) {
            Text(stringResource(Res.string.dialog_confirm_action))
          }
        }
      }
    }
  }
}
