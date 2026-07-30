// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ai.ui.emptystate

import app.campfire.tracing.Trace
import com.r0adkll.cadence.tracer.Tracer

class CadenceTracer : Tracer {
  override fun beginSection(label: String) {
    Trace.beginSection(label)
  }

  override fun endSection() {
    Trace.endSection()
  }
}
