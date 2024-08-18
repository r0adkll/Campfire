package app.campfire.core.di

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
}
