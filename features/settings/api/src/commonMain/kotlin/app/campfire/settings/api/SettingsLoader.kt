// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

/** Reads the stored settings ahead of the first setting anything asks for. */
interface SettingsLoader {

  /** Starts reading the stored settings in the background. Reads made meanwhile wait for it. */
  fun startLoading()
}
