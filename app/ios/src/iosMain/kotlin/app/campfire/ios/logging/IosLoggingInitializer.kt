package app.campfire.ios.logging

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.logging.Heartwood
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@ContributesIntoSet(AppScope::class)
@Inject
class IosLoggingInitializer : AppInitializer {

  override suspend fun onInitialize() {
    Heartwood.grow(IosBark)
  }
}
