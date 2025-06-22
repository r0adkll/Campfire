package app.campfire.libraries.ui.detail.permission

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
