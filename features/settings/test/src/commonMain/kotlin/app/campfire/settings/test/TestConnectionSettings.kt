// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.ConnectionSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [ConnectionSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class TestConnectionSettings : ConnectionSettings {

  private val _socketEnabled = MutableStateFlow<Boolean>(false)
  val socketEnabled: Boolean get() = _socketEnabled.value
  override fun setSocketEnabled(value: Boolean) {
    _socketEnabled.value = value
  }
  override fun observeSocketEnabled(): StateFlow<Boolean> = _socketEnabled.asStateFlow()
}
