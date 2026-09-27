// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesTo

expect interface PreferencesPlatformComponent

@ContributesTo(AppScope::class)
interface SettingsComponent : PreferencesPlatformComponent
