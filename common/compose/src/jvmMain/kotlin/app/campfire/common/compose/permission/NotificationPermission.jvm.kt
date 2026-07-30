// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.permission

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPostNotificationPermissionState(onPermissionResult: (Boolean) -> Unit): PermissionState {
  return PermissionState.Granted
}
