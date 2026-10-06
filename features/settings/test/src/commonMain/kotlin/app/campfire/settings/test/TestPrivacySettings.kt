// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.PrivacySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestPrivacySettings(
  private val testScope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
) : TestSettings(), PrivacySettings {

  override var hasEverConsented: Boolean by boolean()

  override var crashReportingEnabled: Boolean by boolean()
  override fun observeCrashReportingEnabled(): StateFlow<Boolean> =
    observeBoolean(::crashReportingEnabled)
      .stateIn(testScope, SharingStarted.Lazily, crashReportingEnabled)

  override var keepSignedInAfterReinstall: Boolean by boolean()
  override fun observeKeepSignedInAfterReinstall(): StateFlow<Boolean> =
    observeBoolean(::keepSignedInAfterReinstall)
      .stateIn(testScope, SharingStarted.Lazily, keepSignedInAfterReinstall)

  override var analyticReportingEnabled: Boolean by boolean()
  override fun observeAnalyticReportingEnabled(): StateFlow<Boolean> =
    observeBoolean(::analyticReportingEnabled)
      .stateIn(testScope, SharingStarted.Lazily, analyticReportingEnabled)
}
