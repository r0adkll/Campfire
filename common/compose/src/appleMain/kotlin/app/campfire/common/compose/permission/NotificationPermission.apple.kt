package app.campfire.common.compose.permission

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPostNotificationPermissionState(onPermissionResult: (Boolean) -> Unit): PermissionState {
  return PermissionState.Granted
}
