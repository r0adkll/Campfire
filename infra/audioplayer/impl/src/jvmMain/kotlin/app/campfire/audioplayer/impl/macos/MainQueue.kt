// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * Runs work on the AppKit main thread through libdispatch's main queue. The AWT event thread is
 * not the AppKit main thread, and MediaPlayer.framework expects to be driven from the latter.
 */
internal object MainQueue {

  private val system: NativeLibrary = NativeLibrary.getInstance("System")

  /** `dispatch_get_main_queue()` is a macro for the address of this global. */
  private val queue: Pointer = system.getGlobalVariableAddress("_dispatch_main_q")
  private val asyncF: Function = system.getFunction("dispatch_async_f")

  private val pending = ConcurrentLinkedQueue<() -> Unit>()

  /** One long-lived callback that drains [pending]; JNA callbacks must stay strongly referenced. */
  private val drain = DispatchFunction {
    val block = pending.poll() ?: return@DispatchFunction
    try {
      block()
    } catch (t: Throwable) {
      bark(LogPriority.ERROR, throwable = t) { "Main-queue block failed" }
    }
  }

  fun interface DispatchFunction : Callback {
    fun invoke(context: Pointer?)
  }

  fun post(block: () -> Unit) {
    pending.add(block)
    asyncF.invokeVoid(arrayOf(queue, null, drain))
  }

  /** Runs [block] on the main thread and waits for its result; for tests and one-off reads. */
  fun <T> call(timeoutMillis: Long = 5_000, block: () -> T): T {
    val future = CompletableFuture<T>()
    post {
      try {
        future.complete(block())
      } catch (t: Throwable) {
        future.completeExceptionally(t)
      }
    }
    return future.get(timeoutMillis, TimeUnit.MILLISECONDS)
  }
}
