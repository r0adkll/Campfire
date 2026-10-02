// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.initializer

import app.campfire.core.app.AppInitializer
import app.campfire.core.app.UserInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
import app.campfire.settings.api.SettingsLoader
import app.campfire.tracing.StartupTraceSections
import app.campfire.tracing.Trace
import app.campfire.tracing.trace
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@SingleIn(AppScope::class)
@Inject
class StartupInitializer(
  private val settingsLoader: SettingsLoader,
  private val userInitializer: UserInitializer,
  private val initializers: Lazy<Set<AppInitializer>>,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) {

  internal var timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic

  fun initialize() {
    // Everything after this reads settings synchronously, starting with the session restore
    val settingsLoadDuration = Trace.trace(StartupTraceSections.LOAD_SETTINGS) {
      runBlocking {
        measureTime { settingsLoader.load() }
      }
    }
    dbark { "Settings loaded in $settingsLoadDuration" }

    dbark { "--> UserInitializer is starting" }
    val userInitDuration = Trace.trace("UserComponent") {
      runBlocking {
        measureTime { userInitializer.initialize() }
      }
    }
    dbark { "<-- UserInitializer has finished in $userInitDuration" }

    applicationScope.launch {
      ibark { "Starting startup initialization" }

      // Process AppScope Initializers
      val appInitializers = initializers.value.sortedByDescending { it.priority }
      val deferred = appInitializers.map { initializer ->
        processInitializer(initializer)
      }
      deferred.awaitAll()

      ibark { "Finished AppScope initializing" }
    }
  }

  private fun CoroutineScope.processInitializer(initializer: AppInitializer): Deferred<Unit> {
    return async {
      val start = timeSource.markNow()
      dbark { "--> ${initializer::class.simpleName} is starting" }
      try {
        initializer.onInitialize()
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        ebark(throwable = e) { "Something went wrong initializing with ${initializer::class.qualifiedName}" }
      } finally {
        val duration = start.elapsedNow()
        dbark { "<-- ${initializer::class.simpleName} has finished in $duration" }
      }
    }
  }

  companion object : Cork {
    override val tag: String = StartupInitializer::class.simpleName.toString()
  }
}
