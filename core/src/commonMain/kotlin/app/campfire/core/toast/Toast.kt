package app.campfire.core.toast

fun interface Toast {
  enum class Duration {
    SHORT, LONG
  }

  fun show(message: String, duration: Duration): ToastHandle
}

fun interface ToastHandle {
  fun cancel()
}
