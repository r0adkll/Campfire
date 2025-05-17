package app.campfire.debug

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.logging.Heartwood
import app.campfire.debug.events.LogEventCollectorBark
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
@Inject
class DebugAppInitializer(
  private val logEventBark: LogEventCollectorBark,
) : AppInitializer {

  override suspend fun onInitialize() {
    Heartwood.grow(logEventBark)
  }
}
