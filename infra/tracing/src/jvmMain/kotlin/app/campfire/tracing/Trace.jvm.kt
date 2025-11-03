package app.campfire.tracing

actual object Trace {
  actual val isEnabled: Boolean
    get() = false

  actual fun beginSection(label: String) {
  }

  actual fun endSection() {
  }

  actual fun beginAsyncSection(methodName: String, cookie: Int) {
  }

  actual fun endAsyncSection(methodName: String, cookie: Int) {
  }
}
