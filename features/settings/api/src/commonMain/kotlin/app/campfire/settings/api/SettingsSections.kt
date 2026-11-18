// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

/**
 * Which sections Settings offers in this build. The Android Automotive OS app replaces the default
 * with a car's: the car's media app draws the library and player itself, so there's no appearance
 * to choose or book info to look up, and browsing is set up for the car's own media app rather
 * than Android Auto.
 */
interface SettingsSections {
  /** Whether Settings is showing in Campfire for Android Automotive OS. */
  val isCar: Boolean
}
