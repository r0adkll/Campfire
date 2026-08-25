// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import android.content.Context
import androidx.annotation.MainThread
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.ModuleUnavailableException
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the async, Task-based initialization of the shared [CastContext]. The synchronous
 * `CastContext.getSharedInstance(Context)` overload is deprecated and blocks the main thread on
 * IPC + dex-loading of the Play services cast module (a documented ANR source), so all access
 * goes through the async overload and consumers observe [castContext] for readiness.
 *
 * Only [ModuleUnavailableException] locks the feature out for the process — the device has no
 * usable cast module. Any other failure (e.g. Play services mid-update at cold start) is
 * transient: the next [initialize] call retries.
 */
object SafeCastContext {

  private val _castContext = MutableStateFlow<CastContext?>(null)

  /** Emits the shared [CastContext] once the Play services cast module has loaded. */
  val castContext: StateFlow<CastContext?> = _castContext.asStateFlow()

  private var moduleUnavailable = false
  private var initializing = false

  /**
   * Starts (or retries) async initialization. Safe to call repeatedly; no-ops while an attempt
   * is in flight, once initialized, or after the cast module was found to be missing entirely.
   */
  @MainThread
  fun initialize(context: Context) {
    if (_castContext.value != null || moduleUnavailable || initializing) return
    initializing = true

    try {
      val moduleLoadExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "CastContextInit").apply { isDaemon = true }
      }
      CastContext.getSharedInstance(context.applicationContext, moduleLoadExecutor)
        .addOnCompleteListener { task ->
          moduleLoadExecutor.shutdown()
          initializing = false
          if (task.isSuccessful) {
            _castContext.value = task.result
          } else {
            onInitializationFailed(task.exception)
          }
        }
    } catch (e: Throwable) {
      initializing = false
      onInitializationFailed(e)
    }
  }

  /** The shared [CastContext] if initialization has completed, without waiting. */
  fun getIfReady(): CastContext? = _castContext.value

  /** True when the Play services cast module is known to be missing from this device. */
  val isModuleUnavailable: Boolean get() = moduleUnavailable

  private fun onInitializationFailed(error: Throwable?) {
    val isModuleUnavailable = generateSequence(error) { it.cause }
      .any { it is ModuleUnavailableException }
    if (isModuleUnavailable) {
      moduleUnavailable = true
      bark(
        LogPriority.WARN,
        throwable = error,
      ) { "PlayServices Cast module is unavailable, locking the feature out for the session" }
    } else {
      bark(
        LogPriority.ERROR,
        throwable = error,
      ) { "Unable to initialize CastContext; will retry on the next attempt" }
    }
  }
}
