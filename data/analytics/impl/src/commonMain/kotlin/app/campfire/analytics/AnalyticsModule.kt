package app.campfire.analytics

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesTo
import me.tatarka.inject.annotations.Provides

@ContributesTo(AppScope::class)
interface AnalyticsModule {

  @Provides
  @SingleIn(AppScope::class)
  fun provideAnalytics(): Analytics = Analytics.Delegator
}
