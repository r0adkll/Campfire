package app.campfire.tracing

import android.annotation.SuppressLint
import androidx.tracing.Trace as AndroidTrace

actual object Trace {
  actual val isEnabled: Boolean
    get() = AndroidTrace.isEnabled()

  @SuppressLint("UnclosedTrace")
  actual fun beginSection(label: String) = AndroidTrace.beginSection(label)
  actual fun endSection() = AndroidTrace.endSection()

  actual fun beginAsyncSection(methodName: String, cookie: Int) = AndroidTrace.beginAsyncSection(methodName, cookie)
  actual fun endAsyncSection(methodName: String, cookie: Int) = AndroidTrace.endAsyncSection(methodName, cookie)
}
