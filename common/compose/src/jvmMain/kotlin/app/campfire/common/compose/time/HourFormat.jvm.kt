package app.campfire.common.compose.time

import androidx.compose.runtime.Composable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
actual fun is24HourFormat(): Boolean {
  val dateFormat = DateFormat.getTimeInstance(DateFormat.LONG, Locale.getDefault())

  if (dateFormat !is SimpleDateFormat) {
    return false
  }

  return 'H' in dateFormat.toPattern()
}
