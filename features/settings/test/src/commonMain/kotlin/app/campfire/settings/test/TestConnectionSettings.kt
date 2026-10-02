// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.ConnectionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestConnectionSettings(
  private val testScope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
) : TestSettings(), ConnectionSettings {

  override var socketEnabled: Boolean by boolean()
  override fun observeSocketEnabled(): StateFlow<Boolean> =
    observeBoolean(::socketEnabled)
      .stateIn(testScope, SharingStarted.Lazily, socketEnabled)
}
