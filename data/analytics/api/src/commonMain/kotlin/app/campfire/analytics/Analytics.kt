package app.campfire.analytics

import app.campfire.analytics.events.AnalyticEvent
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import kotlin.concurrent.Volatile
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * The main analytics interface to send events through
 */
interface Analytics {

  /**
   * Send an analytics event into the wired analytics pipeline
   */
  fun send(event: AnalyticEvent)

  companion object Delegator : Analytics, SynchronizedObject() {
    override fun send(event: AnalyticEvent) {
      delegatesArray.forEach { it.send(event) }
    }

    operator fun plusAssign(other: Analytics) = synchronized(this) {
      delegates += other
      delegatesArray = delegates.toTypedArray()
    }

    operator fun minusAssign(other: Analytics) = synchronized(this) {
      delegates -= other
      delegatesArray = delegates.toTypedArray()
    }

    private val delegates: MutableList<Analytics> = mutableListOf()

    @Volatile
    private var delegatesArray: Array<Analytics> = emptyArray()
  }
}

object LoggingAnalytics : Analytics {
  override fun send(event: AnalyticEvent) {
    bark(
      priority = LogPriority.INFO,
      tag = "Analytics",
    ) {
      "Event[${event.eventName}]: ${event.params}"
    }
  }
}
