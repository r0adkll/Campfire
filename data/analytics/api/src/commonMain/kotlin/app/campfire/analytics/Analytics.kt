package app.campfire.analytics

import app.campfire.analytics.events.AnalyticEvent
import kotlin.concurrent.Volatile
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * The main analytics interface to send events through
 */
interface Analytics {

  val debugState: String

  /**
   * Send an analytics event into the wired analytics pipeline
   */
  fun send(event: AnalyticEvent)

  companion object Delegator : Analytics, SynchronizedObject() {
    override val debugState: String
      get() = delegatesArray.joinToString("\n") { it.debugState }

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
