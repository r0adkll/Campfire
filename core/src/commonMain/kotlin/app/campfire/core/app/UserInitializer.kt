// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.app

/**
 * A special initializer interface for initializing the current user of the system
 * and instantiating the [app.campfire.core.di.UserScope] part of our DI graph.
 */
interface UserInitializer {

  suspend fun initialize()
}
