package app.campfire.libraries.ui.detail.permission

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPostNotificationPermissionState(onPermissionResult: (Boolean) -> Unit): PermissionState {
  return PermissionState.Granted
}
