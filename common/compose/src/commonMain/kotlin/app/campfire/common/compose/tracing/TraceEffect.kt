// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
    Trace.beginAsyncSectionWithTrackName(trackName, label, cookie)
    onDispose {
      Trace.endAsyncSectionWithTrackName(trackName, label, cookie)
    }
  }
}
