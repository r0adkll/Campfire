package app.campfire.core.toast

actual inline fun runInMainThread(crossinline block: () -> Unit) {
  block()
}
