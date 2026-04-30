package app.campfire.ui.theming.ui.ai.emptystate

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
