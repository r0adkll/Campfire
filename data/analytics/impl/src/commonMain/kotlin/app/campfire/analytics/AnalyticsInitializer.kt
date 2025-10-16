package app.campfire.analytics

import app.campfire.core.app.AppInitializer
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.AppScope
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class AnalyticsInitializer(
  private val applicationInfo: ApplicationInfo,
  private val settings: CampfireSettings,
) : AppInitializer {

  override val priority: Int = AppInitializer.HIGHEST_PRIORITY

  override suspend fun onInitialize() {
    if (applicationInfo.debugBuild) {
      Analytics.Delegator += LoggingAnalytics
    }
  }
}
