// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.permission

import androidx.compose.runtime.Composable

sealed class PermissionState(
  val launchPermissionRequest: () -> Unit = {},
) {
  data object Granted : PermissionState()
  class Denied(requestPermission: () -> Unit) : PermissionState(requestPermission)
  class ShouldShowRationale(requestPermission: () -> Unit) : PermissionState(requestPermission)
}

@Composable
expect fun rememberPostNotificationPermissionState(
  onPermissionResult: (Boolean) -> Unit,
): PermissionState
