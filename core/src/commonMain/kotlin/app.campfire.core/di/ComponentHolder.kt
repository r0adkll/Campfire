package app.campfire.core.di

import app.campfire.core.di.ComponentHolder.components
import app.campfire.core.logging.bark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

/**
 * DI component holder that provides a convenient way to fetch contributed elements on the graph.
 * Take the example where you might contribute a subcomponent,
 * ```
 * @ContributesSubcomponent(
 *   scope = UserScope::class,
 *   parentScope = AppScope::class,
 * )
 * interface UserComponent {
 *   @ContributesSubcomponent.Factory
 *   interface Factory {
 *     fun create(userSession: UserSession): UserComponent
 *   }
 * }
 * ```
 * If you have already added its parent to the [components] set in this holder, then you can fetch the above
 * subcomponent factory like so:
 * ```
 * ComponentHolder.component<UserComponent.Factory>().create(…)
 * ```
 */
object ComponentHolder {

  val components = mutableSetOf<Any>()
  val componentSharedFlow = MutableSharedFlow<Any>(
    replay = 8,
    extraBufferCapacity = 20,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  /**
   * Fetch a component of type [T] that has been added to the holder, automatically casting
   * it in the return.
   */
  inline fun <reified T> component(): T {
    return components
      .filterIsInstance<T>()
      .firstOrNull()
      ?: throw NoSuchElementException("No component found for '${T::class.qualifiedName}'")
  }

  /**
   * Fetch a component of type [T] that has been added to the holder, automatically casting
   * it in the return. If this component doesn't exist, then return null
   */
  inline fun <reified T> maybeComponent(): T? {
    return components
      .filterIsInstance<T>()
      .firstOrNull()
  }

  /**
   * Update a component of the given type, [T], in the component holder
   */
  fun <T : Any> updateComponent(scope: CoroutineScope, component: T) {
    components.removeAll { it::class.isInstance(component) }
    components += component

    scope.launch {
      componentSharedFlow.emit(component)
      bark("ComponentHolder") { "updateComponent($component) - Success" }
    }
  }

  inline fun <reified T> subscribe(): Flow<T> {
    return componentSharedFlow
      .onSubscription {
        val existing = maybeComponent<T>()
        if (existing != null) {
          emit(existing)
        }
      }
      .filterIsInstance<T>()
  }
}
