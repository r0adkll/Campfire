// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.glance.LocalContext

@Composable
internal fun glanceStringResource(
  @StringRes resId: Int,
): String {
  return LocalContext.current.getString(resId)
}
