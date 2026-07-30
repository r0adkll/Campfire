// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
