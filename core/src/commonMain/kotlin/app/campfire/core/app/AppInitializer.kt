package app.campfire.core.app

interface AppInitializer {
  val priority: Int get() = DEFAULT_PRIORITY

  suspend fun onInitialize()

  companion object {
    const val LOWEST_PRIORITY = Int.MIN_VALUE
    const val DEFAULT_PRIORITY = 0
    const val HIGHEST_PRIORITY = Int.MAX_VALUE - 10

    /*
     * This is a place for explicitly defining highest-priority intializers
     *
     * Remove with #471
     */

    const val FIREBASE_INIT_PRIORITY = Int.MAX_VALUE - 1

    /**
     * This is a special zone for initializers that are deliberately
     * started before ANYTHING ELSE.
     *
     * This is gross and is to be fixed with #471
     */
    val FIRST_INIT_PRIORITY_RANGE
      get() = (HIGHEST_PRIORITY + 1) until Int.MAX_VALUE
  }
}
