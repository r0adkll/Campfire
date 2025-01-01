package app.campfire.core.app

interface AppInitializer {

  suspend fun onInitialize()
}
