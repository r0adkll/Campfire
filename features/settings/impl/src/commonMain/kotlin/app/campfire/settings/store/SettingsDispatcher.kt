// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import dev.zacsweers.metro.Qualifier

/**
 * The dispatcher every settings read and write runs on: off the main thread, one task at a time,
 * in submission order.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsDispatcher
