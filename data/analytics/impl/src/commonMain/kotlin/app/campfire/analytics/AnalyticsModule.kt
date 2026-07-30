// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
