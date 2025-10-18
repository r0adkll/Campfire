package app.campfire.analytics.mixpanel

import app.campfire.analytics.Analytics
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class MixPanelInitializer(
  private val settings: CampfireSettings,
  private val mixPanelFacadeLazy: Lazy<MixPanelFacade>,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : AppInitializer {

  override val priority: Int = AppInitializer.HIGHEST_PRIORITY - 1

  private val mixPanelScope = CoroutineScope(SupervisorJob() + applicationScope.coroutineContext) +
    CoroutineExceptionHandler { _, error ->
      bark(LogPriority.ERROR, throwable = error) { "Something went wrong with MixPanel" }
    }

  override suspend fun onInitialize() {
    if (BuildConfig.MIXPANEL_TOKEN == null) return

    mixPanelFacadeLazy.value.identify(
      distinctId = settings.analyticsId,
    )

    observeAnalyticsSetting()
  }

  private fun observeAnalyticsSetting() {
    mixPanelScope.launch {
      // Make sure we initialize on Background thread
      val mixPanelFacade = mixPanelFacadeLazy.value
      val mixPanelAnalytics = MixPanelAnalytics(mixPanelFacade)

      settings
        .observeAnalyticReportingEnabled()
        .collect { analyticReportingEnabled ->
          bark("MixPanel") { "Analytic Reporting - Enabled[$analyticReportingEnabled]" }

          // Add/Remove our own tracking from the delegator
          if (analyticReportingEnabled) {
            Analytics.Delegator += mixPanelAnalytics
          } else {
            Analytics.Delegator -= mixPanelAnalytics
          }

          // Make sure the SDK isn't reporting events
          if (analyticReportingEnabled && mixPanelFacade.isOptOut) {
            mixPanelFacade.optIn()
          } else if (!analyticReportingEnabled && !mixPanelFacade.isOptOut) {
            mixPanelFacade.optOut()
          }
        }
    }
  }
}
