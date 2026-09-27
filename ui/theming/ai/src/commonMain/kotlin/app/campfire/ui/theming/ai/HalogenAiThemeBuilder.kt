// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ai

import app.campfire.core.di.AppScope
import app.campfire.ui.theming.api.AiThemeBuilder
import app.campfire.ui.theming.ui.picker.UnsupportedAiThemeBuilder
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Real [AiThemeBuilder] contributed when this module is included in the build. Presence
 * of this binding is what flips [isSupported] on — FOSS builds exclude the module and
 * fall back to [UnsupportedAiThemeBuilder].
 */
@ContributesBinding(AppScope::class, replaces = [UnsupportedAiThemeBuilder::class])
@Inject
class HalogenAiThemeBuilder(
  private val halogenThemeManager: HalogenThemeManager,
) : AiThemeBuilder {

  override val isSupported: Boolean = true

  override fun observeIsAvailable(): StateFlow<Boolean> = halogenThemeManager.observeIsAvailable()
}
