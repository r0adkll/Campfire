// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.tracing

/**
 * Trace section labels for startup work outside dependency injection. The startup macrobenchmark
 * (`:app:baselineprofile`) measures these by name, so keep them stable.
 */
object StartupTraceSections {
  /** The main thread waiting for the settings to finish loading; the read itself starts earlier. */
  const val LOAD_SETTINGS = "startup:loadSettings"
}
