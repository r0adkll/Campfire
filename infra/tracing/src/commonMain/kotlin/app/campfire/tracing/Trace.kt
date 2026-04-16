package app.campfire.tracing

expect object Trace {

  val isEnabled: Boolean

  fun beginSection(label: String)
  fun endSection()

  fun beginAsyncSection(methodName: String, cookie: Int)
  fun endAsyncSection(methodName: String, cookie: Int)

  fun beginAsyncSectionWithTrackName(trackName: String, methodName: String, cookie: Int)
  fun endAsyncSectionWithTrackName(trackName: String, methodName: String, cookie: Int)
}

inline fun <T> Trace.trace(label: String, block: () -> T): T {
  beginSection(label)
  return try {
    block()
  } finally {
    endSection()
  }
}
