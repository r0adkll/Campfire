package app.campfire.shared.initializer

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@Inject
class StartupInitializer(
  initializers: Set<AppInitializer>,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) {

  private val sortedInitializers = initializers.sortedByDescending { it.priority }

  fun initialize() {
    applicationScope.launch {
      ibark { "Starting startup initialization" }
      val deferred = sortedInitializers.map { initializer ->
        async {
          dbark { "--> ${initializer::class.simpleName} is starting" }
          try {
            initializer.onInitialize()
          } catch (e: Exception) {
            if (e is CancellationException) throw e
            ebark(throwable = e) { "Something went wrong initializing with ${initializer::class.qualifiedName}" }
          } finally {
            dbark { "<-- ${initializer::class.simpleName} has finished" }
          }
        }
      }

      deferred.awaitAll()
      ibark { "Finished startup initializing" }
    }
  }

  companion object : Cork {
    override val tag: String = StartupInitializer::class.simpleName.toString()
  }
}
