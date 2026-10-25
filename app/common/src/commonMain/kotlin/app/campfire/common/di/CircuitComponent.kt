// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.di

import app.campfire.analytics.Analytics
import app.campfire.common.navigator.AnalyticsNavigationEventListener
import app.campfire.common.navigator.LoggingNavigationEventListener
import app.campfire.core.di.UserScope
import app.campfire.crashreporting.CrashReporter
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuitx.navigation.intercepting.NavigationEventListener
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@ContributesTo(UserScope::class)
interface CircuitComponent {

  @Provides
  @SingleIn(UserScope::class)
  fun provideCircuit(
    uiFactories: Set<Ui.Factory>,
    presenterFactories: Set<Presenter.Factory>,
  ): Circuit = Circuit.Builder()
    .addUiFactories(uiFactories)
    .addPresenterFactories(presenterFactories)
    .build()

  /**
   * One list per user: the root reads it whenever it recomposes, and a navigator built from it is
   * rebuilt whenever the list is a new one, along with the presenter and UI of the screen it shows.
   */
  @Provides
  @SingleIn(UserScope::class)
  fun provideNavigationEventListeners(): ImmutableList<NavigationEventListener> = persistentListOf(
    LoggingNavigationEventListener,
    AnalyticsNavigationEventListener(
      analytics = Analytics.Delegator,
      crashReporter = CrashReporter.Delegator,
    ),
  )
}
