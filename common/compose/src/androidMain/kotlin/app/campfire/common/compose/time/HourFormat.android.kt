package app.campfire.common.compose.time

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun is24HourFormat(): Boolean {
  val androidContext = LocalContext.current
  return DateFormat.is24HourFormat(androidContext)
}
