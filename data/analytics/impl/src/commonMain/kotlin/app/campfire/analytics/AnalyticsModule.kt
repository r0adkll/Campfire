// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.analytics

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface AnalyticsModule {

  @Provides
  @SingleIn(AppScope::class)
  fun provideAnalytics(): Analytics = Analytics.Delegator
}
