package app.campfire.tracing

expect object Trace {

  val isEnabled: Boolean

  fun beginSection(label: String)
  fun endSection()

  fun beginAsyncSection(methodName: String, cookie: Int)
  fun endAsyncSection(methodName: String, cookie: Int)
}
