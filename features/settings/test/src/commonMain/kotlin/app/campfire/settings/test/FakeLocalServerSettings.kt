// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.LocalServerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A simple in-memory [LocalServerSettings] fake backed by a [MutableStateFlow] for use in tests.
 */
class FakeLocalServerSettings : LocalServerSettings {

  private val _avoidMobileData = MutableStateFlow(true)
  override var avoidMobileData: Boolean
    get() = _avoidMobileData.value
    set(value) { _avoidMobileData.value = value }
  override fun observeAvoidMobileData(): StateFlow<Boolean> = _avoidMobileData.asStateFlow()
}
