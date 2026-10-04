// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.MobileDataSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [MobileDataSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeMobileDataSettings : MobileDataSettings {

  private val _homeServerOnMobileData = MutableStateFlow<Boolean>(false)
  override val homeServerOnMobileData: Boolean get() = _homeServerOnMobileData.value
  override fun setHomeServerOnMobileData(value: Boolean) {
    _homeServerOnMobileData.value = value
  }
  override fun observeHomeServerOnMobileData(): StateFlow<Boolean> = _homeServerOnMobileData.asStateFlow()

  private val _downloadOnWifiOnly = MutableStateFlow<Boolean>(false)
  override val downloadOnWifiOnly: Boolean get() = _downloadOnWifiOnly.value
  override fun setDownloadOnWifiOnly(value: Boolean) {
    _downloadOnWifiOnly.value = value
  }
  override fun observeDownloadOnWifiOnly(): StateFlow<Boolean> = _downloadOnWifiOnly.asStateFlow()
}
