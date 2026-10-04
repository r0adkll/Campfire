// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.Flow

/**
 * The app theme the signed-in account picked. Bound in the user graph, so each account has its own; signed
 * out, it's the default. Light/dark mode stays app-wide in [ThemeSettings].
 */
interface UserThemeSettings {
  fun observeThemeId(): Flow<ThemeKey>
  fun setThemeId(value: ThemeKey)
}
