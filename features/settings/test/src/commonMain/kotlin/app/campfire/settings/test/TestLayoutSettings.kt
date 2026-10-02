// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.LayoutSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestLayoutSettings(
  private val testScope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
) : TestSettings(), LayoutSettings {

  override var wideNavigationRailExpanded: Boolean by boolean()
  override fun observeWideNavigationRailExpanded(): StateFlow<Boolean> =
    observeBoolean(::wideNavigationRailExpanded)
      .stateIn(testScope, SharingStarted.Lazily, wideNavigationRailExpanded)

  override var supportingPaneWidth: Float by float()
  override fun observeSupportingPaneWidth(): StateFlow<Float> =
    observeFloat(::supportingPaneWidth)
      .stateIn(testScope, SharingStarted.Lazily, supportingPaneWidth)
}
