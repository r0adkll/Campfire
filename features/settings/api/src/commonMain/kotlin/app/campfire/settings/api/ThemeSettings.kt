// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.Flow

interface ThemeSettings {

  fun setDynamicallyThemeItemDetail(value: Boolean)
  fun observeDynamicallyThemeItemDetail(): Flow<Boolean>

  fun setDynamicallyThemePlayback(value: Boolean)
  fun observeDynamicallyThemePlayback(): Flow<Boolean>

  fun observeThemeId(): Flow<ThemeKey>
  fun setThemeId(value: ThemeKey)

  fun setThemeMode(value: ThemeMode)
  fun observeTheme(): Flow<ThemeMode>

  /**
   * The stored theme mode, read without waiting so the app's first frame is drawn in it. Use
   * [observeTheme] everywhere else.
   */
  fun lastThemeMode(): ThemeMode

  companion object {
    const val DEFAULT_DYNAMICALLY_THEME_ITEM_DETAIL: Boolean = true
    const val DEFAULT_DYNAMICALLY_THEME_PLAYBACK: Boolean = true
  }
}
