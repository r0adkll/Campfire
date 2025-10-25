package app.campfire.analytics.mixpanel.di

import app.campfire.analytics.mixpanel.MixPanelFacade
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import me.tatarka.inject.annotations.Provides

actual interface PlatformMixPanelComponent {
  @Provides
  @SingleIn(AppScope::class)
  fun provideMixPanelFacade() = MixPanelFacade()
}
