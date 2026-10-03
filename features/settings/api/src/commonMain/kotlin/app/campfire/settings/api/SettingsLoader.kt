// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

/**
 * Loads the stored settings into memory. Startup awaits [load] before anything reads a setting, so the
 * settings can be read synchronously from then on.
 */
interface SettingsLoader {

  /**
   * Starts reading the stored settings in the background, so [load] has less, or nothing, left to wait for.
   * Calling it again does nothing.
   */
  fun startLoading()

  /** Returns once the settings are in memory, starting the read first if [startLoading] hasn't. */
  suspend fun load()
}
