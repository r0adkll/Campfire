package app.campfire.tracing

import android.os.Trace
import java.lang.reflect.Method

internal class AsyncTrackTracer {
  private val beginTrace: (String, String, Int) -> Unit by lazy(::asyncTraceForTrackBegin)
  private val endTrace: (String, String, Int) -> Unit by lazy(::asyncTraceForTrackEnd)

  fun beginAsyncTraceForTrack(trackName: String, methodName: String, cookie: Int) {
    beginTrace(trackName, methodName, cookie)
  }

  fun endAsyncTraceForTrack(trackName: String, methodName: String, cookie: Int) {
    endTrace(trackName, methodName, cookie)
  }

  private fun asyncTraceForTrackBegin(): (trackName: String, methodName: String, cookie: Int) -> Unit {
    val method = hiddenTraceMethod(
      "asyncTraceForTrackBegin",
      Long::class.java,
      String::class.java,
      String::class.java,
      Int::class.java,
    )

    if (method != null) {
      return { trackName, methodName, cookie ->
        method.invoke(null, traceTagApp, trackName, methodName, cookie)
      }
    } else {
      return { _, methodName, cookie ->
        Trace.beginAsyncSection(methodName, cookie)
      }
    }
  }

  private fun asyncTraceForTrackEnd(): (trackName: String, methodName: String, cookie: Int) -> Unit {
    val methodApi34 = hiddenTraceMethod(
      "asyncTraceForTrackEnd",
      Long::class.java,
      String::class.java,
      Int::class.java,
    )

    if (methodApi34 != null) {
      return { trackName, _, cookie ->
        methodApi34.invoke(null, traceTagApp, trackName, cookie)
      }
    }

    val methodCompat = hiddenTraceMethod(
      "asyncTraceForTrackEnd",
      Long::class.java,
      String::class.java,
      String::class.java,
      Int::class.java,
    )

    if (methodCompat != null) {
      return { trackName, methodName, cookie ->
        methodCompat.invoke(null, traceTagApp, trackName, methodName, cookie)
      }
    }

    return { _, methodName, cookie ->
      Trace.endAsyncSection(methodName, cookie)
    }
  }

  private fun hiddenTraceMethod(name: String, vararg params: Class<*>): Method? {
    return try {
      Trace::class.java.getDeclaredMethod(name, *params)
        .also { it.isAccessible = true }
    } catch (_: Throwable) {
      null
    }
  }

  private val traceTagApp: Long by lazy {
    Trace::class.java.declaredFields.firstOrNull { it.name == "TRACE_TAG_APP" }?.let {
      it.isAccessible = true
      it.getLong(null)
    } ?: (1L shl 12) // Fall back to default value
  }
}
