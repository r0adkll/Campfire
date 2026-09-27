// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import app.campfire.core.di.AppScope
import app.campfire.ui.theming.api.AiThemeBuilder
import app.campfire.ui.theming.api.NoOpAiThemeBuilder
import dev.zacsweers.metro.ContributesBinding

/**
 * The [AiThemeBuilder] for builds without the optional `:ui:theming:ai` module (e.g. FOSS),
 * which replaces this binding with the real implementation when it is included.
 */
@ContributesBinding(AppScope::class)
class UnsupportedAiThemeBuilder : AiThemeBuilder by NoOpAiThemeBuilder
