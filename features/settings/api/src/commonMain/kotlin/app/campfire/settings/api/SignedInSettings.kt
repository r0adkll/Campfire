// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.Flow

/**
 * The signed-in account's settings, for app-wide code that outlives the user graph, such as the players and
 * the theme repository. Each call reaches the current user graph, so a value read or written always belongs to
 * whoever is signed in at that moment. Code inside the user graph injects the settings directly instead.
 */
interface SignedInSettings {

  fun perBook(): PerBookSettings

  fun theme(): UserThemeSettings

  /** The signed-in account's theme, then the next account's whenever the account changes. */
  fun observeThemeId(): Flow<ThemeKey>
}
