package app.campfire.common.compose.toast

import androidx.compose.runtime.staticCompositionLocalOf
import app.campfire.core.logging.bark

/**
 * Access the local toast framework in the current composition.
 */
val LocalToast = staticCompositionLocalOf<Toast> { NoOpToast }

fun interface Toast {
  enum class Duration {
    SHORT, LONG
  }

  fun show(message: String, duration: Duration): ToastHandle
}

fun interface ToastHandle {
  fun cancel()
}

private object NoOpToast : Toast {

  override fun show(message: String, duration: Toast.Duration): ToastHandle {
    bark { "\uD83C\uDF5E: $message" }
    return ToastHandle {}
  }
}
