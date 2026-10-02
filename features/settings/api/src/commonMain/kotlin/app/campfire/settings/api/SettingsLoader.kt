// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

/**
 * Loads the stored settings into memory. Startup awaits this before anything reads a setting, so the
 * settings can be read synchronously from then on.
 */
fun interface SettingsLoader {
  suspend fun load()
}
