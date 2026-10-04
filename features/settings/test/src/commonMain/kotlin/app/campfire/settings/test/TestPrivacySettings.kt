// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.PrivacySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [PrivacySettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class TestPrivacySettings : PrivacySettings {

  private val _hasEverConsented = MutableStateFlow<Boolean>(false)
  override val hasEverConsented: Boolean get() = _hasEverConsented.value
  override fun setHasEverConsented(value: Boolean) {
    _hasEverConsented.value = value
  }
  override fun observeHasEverConsented(): StateFlow<Boolean> = _hasEverConsented.asStateFlow()

  private val _crashReportingEnabled = MutableStateFlow<Boolean>(false)
  override val crashReportingEnabled: Boolean get() = _crashReportingEnabled.value
  override fun setCrashReportingEnabled(value: Boolean) {
    _crashReportingEnabled.value = value
  }
  override fun observeCrashReportingEnabled(): StateFlow<Boolean> = _crashReportingEnabled.asStateFlow()

  private val _keepSignedInAfterReinstall = MutableStateFlow<Boolean>(false)
  override val keepSignedInAfterReinstall: Boolean get() = _keepSignedInAfterReinstall.value
  override fun setKeepSignedInAfterReinstall(value: Boolean) {
    _keepSignedInAfterReinstall.value = value
  }
  override fun observeKeepSignedInAfterReinstall(): StateFlow<Boolean> = _keepSignedInAfterReinstall.asStateFlow()

  private val _analyticReportingEnabled = MutableStateFlow<Boolean>(false)
  override val analyticReportingEnabled: Boolean get() = _analyticReportingEnabled.value
  override fun setAnalyticReportingEnabled(value: Boolean) {
    _analyticReportingEnabled.value = value
  }
  override fun observeAnalyticReportingEnabled(): StateFlow<Boolean> = _analyticReportingEnabled.asStateFlow()
}
