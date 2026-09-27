// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.analytics.mixpanel.di

import app.campfire.analytics.mixpanel.MixPanelFacade
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual interface PlatformMixPanelComponent {
  @Provides
  @SingleIn(AppScope::class)
  fun provideMixPanelFacade(): MixPanelFacade = MixPanelFacade()
}
