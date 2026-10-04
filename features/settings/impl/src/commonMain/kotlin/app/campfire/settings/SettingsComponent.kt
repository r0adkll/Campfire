// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.settings.store.PreferencesPlatformComponent
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
interface SettingsComponent : PreferencesPlatformComponent
