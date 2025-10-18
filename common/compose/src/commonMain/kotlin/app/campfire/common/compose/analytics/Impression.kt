package app.campfire.common.compose.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.AnalyticEvent

@Composable
fun Impression(
  event: () -> AnalyticEvent,
) {
  Impression(
    Unit,
    event,
  )
}

@Composable
fun Impression(
  key1: Any?,
  event: () -> AnalyticEvent,
) {
  LaunchedEffect(key1) {
    Analytics.send(event())
  }
}
