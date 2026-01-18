package app.campfire.common.compose.toast

import androidx.compose.runtime.staticCompositionLocalOf
import app.campfire.core.logging.bark
import app.campfire.core.toast.Toast
import app.campfire.core.toast.ToastHandle

/**
 * Access the local toast framework in the current composition.
 */
val LocalToast = staticCompositionLocalOf<Toast> { NoOpToast }

private object NoOpToast : Toast {

  override fun show(message: String, duration: Toast.Duration): ToastHandle {
    bark { "\uD83C\uDF5E: $message" }
    return ToastHandle {}
  }
}
