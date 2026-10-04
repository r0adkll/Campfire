// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.ThemeMode
import app.campfire.settings.api.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [ThemeSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class TestThemeSettings : ThemeSettings {

  private val _dynamicallyThemeItemDetail = MutableStateFlow<Boolean>(false)
  val dynamicallyThemeItemDetail: Boolean get() = _dynamicallyThemeItemDetail.value
  override fun setDynamicallyThemeItemDetail(value: Boolean) {
    _dynamicallyThemeItemDetail.value = value
  }
  override fun observeDynamicallyThemeItemDetail(): StateFlow<Boolean> = _dynamicallyThemeItemDetail.asStateFlow()

  private val _dynamicallyThemePlayback = MutableStateFlow<Boolean>(false)
  val dynamicallyThemePlayback: Boolean get() = _dynamicallyThemePlayback.value
  override fun setDynamicallyThemePlayback(value: Boolean) {
    _dynamicallyThemePlayback.value = value
  }
  override fun observeDynamicallyThemePlayback(): StateFlow<Boolean> = _dynamicallyThemePlayback.asStateFlow()

  private val _themeMode = MutableStateFlow<ThemeMode>(ThemeMode.entries.first())
  val themeMode: ThemeMode get() = _themeMode.value
  override fun setThemeMode(value: ThemeMode) {
    _themeMode.value = value
  }
  override fun observeTheme(): StateFlow<ThemeMode> = _themeMode.asStateFlow()

  override fun lastThemeMode(): ThemeMode = themeMode
}
