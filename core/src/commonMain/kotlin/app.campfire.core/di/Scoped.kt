package app.campfire.core.di

/**
 *
 */
interface Scoped {

  suspend fun onCreate() {}
  suspend fun onDestroy() {}
}
