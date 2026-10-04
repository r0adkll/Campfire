// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

interface ThemeSettings {

  val dynamicallyThemeItemDetail: Boolean
  fun setDynamicallyThemeItemDetail(value: Boolean)
  fun observeDynamicallyThemeItemDetail(): StateFlow<Boolean>

  val dynamicallyThemePlayback: Boolean
  fun setDynamicallyThemePlayback(value: Boolean)
  fun observeDynamicallyThemePlayback(): StateFlow<Boolean>

  val themeId: ThemeKey
  fun observeThemeId(): StateFlow<ThemeKey>
  fun setThemeId(value: ThemeKey)

  val themeMode: ThemeMode
  fun setThemeMode(value: ThemeMode)
  fun observeTheme(): StateFlow<ThemeMode>
}
