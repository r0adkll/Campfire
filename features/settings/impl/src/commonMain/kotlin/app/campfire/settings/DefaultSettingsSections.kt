// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.settings.api.SettingsSections
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Every section, for phones, tablets and desktops. */
@ContributesBinding(AppScope::class)
@Inject
class DefaultSettingsSections : SettingsSections {
  override val isCar: Boolean = false
}
