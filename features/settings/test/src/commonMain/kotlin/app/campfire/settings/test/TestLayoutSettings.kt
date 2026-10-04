// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.LayoutSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [LayoutSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class TestLayoutSettings : LayoutSettings {

  private val _wideNavigationRailExpanded = MutableStateFlow<Boolean>(false)
  val wideNavigationRailExpanded: Boolean get() = _wideNavigationRailExpanded.value
  override fun setWideNavigationRailExpanded(value: Boolean) {
    _wideNavigationRailExpanded.value = value
  }
  override fun observeWideNavigationRailExpanded(): StateFlow<Boolean> = _wideNavigationRailExpanded.asStateFlow()

  private val _supportingPaneWidth = MutableStateFlow<Float>(0f)
  val supportingPaneWidth: Float get() = _supportingPaneWidth.value
  override fun setSupportingPaneWidth(value: Float) {
    _supportingPaneWidth.value = value
  }
  override fun observeSupportingPaneWidth(): StateFlow<Float> = _supportingPaneWidth.asStateFlow()
}
