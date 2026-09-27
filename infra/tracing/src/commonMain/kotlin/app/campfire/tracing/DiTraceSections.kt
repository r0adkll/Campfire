// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.tracing

/**
 * Trace section labels wrapped around dependency graph creation. The DI startup macrobenchmark
 * (`:app:baselineprofile`) measures these by name, so keep them stable across DI frameworks.
 */
object DiTraceSections {
  const val APP_GRAPH = "di:createAppGraph"
  const val ACTIVITY_GRAPH = "di:createActivityGraph"
  const val USER_GRAPH = "di:createUserGraph"
}
