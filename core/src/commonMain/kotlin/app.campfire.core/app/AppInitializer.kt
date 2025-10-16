package app.campfire.core.app

interface AppInitializer {
  val priority: Int get() = DEFAULT_PRIORITY

  suspend fun onInitialize()

  companion object {
    const val LOWEST_PRIORITY = Int.MIN_VALUE
    const val DEFAULT_PRIORITY = 0
    const val HIGHEST_PRIORITY = Int.MAX_VALUE - 10
  }
}
