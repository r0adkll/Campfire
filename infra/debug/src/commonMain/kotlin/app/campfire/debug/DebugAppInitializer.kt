package app.campfire.debug

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Heartwood
import app.campfire.debug.events.LogEventCollectorBark
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesMultibinding(AppScope::class)
@Inject
class DebugAppInitializer(
  private val logEventBark: LogEventCollectorBark,
) : AppInitializer {

  override suspend fun onInitialize() {
    Heartwood.grow(logEventBark)
  }
}
