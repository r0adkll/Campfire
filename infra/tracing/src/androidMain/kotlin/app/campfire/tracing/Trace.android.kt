// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.tracing

import android.annotation.SuppressLint
import androidx.tracing.Trace as AndroidTrace

actual object Trace {

  private val asyncTrackTracer by lazy { AsyncTrackTracer() }

  actual val isEnabled: Boolean
    get() = AndroidTrace.isEnabled()

  @SuppressLint("UnclosedTrace")
  actual fun beginSection(label: String) = AndroidTrace.beginSection(label)
  actual fun endSection() = AndroidTrace.endSection()

  actual fun beginAsyncSection(methodName: String, cookie: Int) = AndroidTrace.beginAsyncSection(methodName, cookie)
  actual fun endAsyncSection(methodName: String, cookie: Int) = AndroidTrace.endAsyncSection(methodName, cookie)

  actual fun beginAsyncSectionWithTrackName(trackName: String, methodName: String, cookie: Int) {
    asyncTrackTracer.beginAsyncTraceForTrack(trackName, methodName, cookie)
  }

  actual fun endAsyncSectionWithTrackName(trackName: String, methodName: String, cookie: Int) {
    asyncTrackTracer.endAsyncTraceForTrack(trackName, methodName, cookie)
  }
}
