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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.time.is24HourFormat
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.dialog_confirm_action
import campfire.features.settings.ui.generated.resources.dialog_dismiss_action
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LocalTimeSetting(
  value: LocalTime,
  onValueChange: (LocalTime) -> Unit,
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
        text = value.format(localTimeFormatter()),
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
    LocalTimePickerDialog(
      current = value,
      onConfirm = onValueChange,
      onDismissRequest = { showDialog = false },
      title = headlineContent,
    )
  }
}

@Composable
private fun localTimeFormatter(): DateTimeFormat<LocalTime> {
  return if (is24HourFormat()) {
    LocalTime.Format {
      hour(Padding.NONE); char(':'); minute()
    }
  } else {
    LocalTime.Format {
      amPmHour(Padding.NONE); char(':'); minute(); char(' '); amPmMarker("AM", "PM")
    }
  }
}

@Composable
private fun LocalTimePickerDialog(
  current: LocalTime,
  onConfirm: (LocalTime) -> Unit,
  onDismissRequest: () -> Unit,
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  val windowSizeClass = LocalWindowSizeClass.current
  BasicAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  ) {
    val timePickerState = rememberTimePickerState(
      initialHour = current.hour,
      initialMinute = current.minute,
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

        TimePicker(
          state = timePickerState,
          layoutType = if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded) {
            TimePickerLayoutType.Horizontal
          } else {
            TimePickerLayoutType.Vertical
          },
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
            enabled = timePickerState.hour != current.hour ||
              timePickerState.minute != current.minute,
            onClick = {
              val localTime = LocalTime(
                hour = timePickerState.hour,
                minute = timePickerState.minute,
              )
              onConfirm(localTime)
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
