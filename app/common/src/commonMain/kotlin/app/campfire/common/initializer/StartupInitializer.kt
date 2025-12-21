package app.campfire.common.initializer

import app.campfire.core.app.AppInitializer
import app.campfire.core.app.UserInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@Inject
class StartupInitializer(
  private val userInitializer: UserInitializer,
  private val initializers: Set<AppInitializer>,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) {

  internal var timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic

  fun initialize() {
    applicationScope.launch {
      ibark { "Starting startup initialization" }

      dbark { "--> UserInitializer is starting" }
      val userInitDuration = measureTime { userInitializer.initialize() }
      dbark { "<-- UserInitializer has finished in $userInitDuration" }

      // Process AppScope Initializers
      val appInitializers = initializers.sortedByDescending { it.priority }
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
