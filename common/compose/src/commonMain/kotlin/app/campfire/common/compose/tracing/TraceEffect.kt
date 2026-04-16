package app.campfire.common.compose.tracing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import app.campfire.tracing.Trace

const val DefaultUiTrackName = "UI"

/**
 * A Composable SideEffect for logging trace calls for composable screens
 */
@Composable
fun TraceEffect(
  label: String,
  trackName: String = DefaultUiTrackName,
  cookie: Int = 0,
) {
  DisposableEffect(Unit) {
    // TODO: Replace this when asyncSectionWithTrackName is correctly implemented
    val methodName = "$trackName: $label"
    Trace.beginAsyncSectionWithTrackName(trackName, methodName, cookie)
    onDispose {
      Trace.endAsyncSectionWithTrackName(trackName, methodName, cookie)
    }
  }
}
