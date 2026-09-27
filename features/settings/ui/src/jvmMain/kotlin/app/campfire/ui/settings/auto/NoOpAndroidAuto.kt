// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.auto

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class NoOpAndroidAuto : AndroidAuto {
  override fun isAvailable(): Boolean = false
  override fun openSettings() = Unit
}
