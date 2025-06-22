package app.campfire.libraries.ui.detail.permission

import android.os.Build
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberPostNotificationPermissionState(
  onPermissionResult: (Boolean) -> Unit,
): PermissionState {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return PermissionState.Granted
  val androidPermissionState = rememberPermissionState(
    android.Manifest.permission.POST_NOTIFICATIONS,
    onPermissionResult = onPermissionResult,
  )
  return when {
    androidPermissionState.status.isGranted -> PermissionState.Granted
    androidPermissionState.status.shouldShowRationale ->
      PermissionState.ShouldShowRationale(androidPermissionState::launchPermissionRequest)

    else -> PermissionState.Denied(androidPermissionState::launchPermissionRequest)
  }
}
