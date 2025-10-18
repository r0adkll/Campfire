package app.campfire.crashreporting

import kotlin.concurrent.Volatile
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

interface CrashReporter {

  fun tag(key: String, tag: String)
  fun record(t: Throwable)

  companion object Delegator : CrashReporter, SynchronizedObject() {
    override fun tag(key: String, tag: String) {
      delegatesArray.forEach { it.tag(key, tag) }
    }

    override fun record(t: Throwable) {
      delegatesArray.forEach { it.record(t) }
    }

    operator fun plusAssign(other: CrashReporter) = synchronized(this) {
      delegates += other
      delegatesArray = delegates.toTypedArray()
    }

    operator fun minusAssign(other: CrashReporter) = synchronized(this) {
      delegates -= other
      delegatesArray = delegates.toTypedArray()
    }

    private val delegates: MutableList<CrashReporter> = mutableListOf()

    @Volatile
    private var delegatesArray: Array<CrashReporter> = emptyArray()
  }
}
