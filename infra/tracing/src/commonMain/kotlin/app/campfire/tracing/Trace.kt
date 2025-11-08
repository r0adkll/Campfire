package app.campfire.tracing

expect object Trace {

  val isEnabled: Boolean

  fun beginSection(label: String)
  fun endSection()

  fun beginAsyncSection(methodName: String, cookie: Int)
  fun endAsyncSection(methodName: String, cookie: Int)
}

inline fun <R> trace(label: String, block: () -> R): R {
  Trace.beginSection(label)
  return try {
    block()
  } finally {
    Trace.endSection()
  }
}
