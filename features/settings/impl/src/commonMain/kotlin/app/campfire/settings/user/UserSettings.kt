// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import dev.zacsweers.metro.Qualifier

/** Qualifies the signed-in account's [app.campfire.settings.store.SettingsStore], in the user graph. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserSettings
