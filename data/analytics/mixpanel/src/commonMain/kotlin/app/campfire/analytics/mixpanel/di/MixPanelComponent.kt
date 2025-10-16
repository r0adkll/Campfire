package app.campfire.analytics.mixpanel.di

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(AppScope::class)
interface MixPanelComponent : PlatformMixPanelComponent

expect interface PlatformMixPanelComponent
