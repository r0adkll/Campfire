// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.time

import androidx.compose.runtime.Composable
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

@Composable
actual fun is24HourFormat(): Boolean {
  return NSDateFormatter
    .dateFormatFromTemplate("j", 0.toULong(), NSLocale.currentLocale())
    ?.contains('a') == false
}
