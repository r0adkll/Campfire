// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.di

import app.campfire.common.root.CampfireContentWithInsets
import app.campfire.common.root.MiniPlayerContent
import app.campfire.core.di.AppScope
import app.campfire.core.di.UiScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension

@GraphExtension(UiScope::class)
interface WindowComponent {
  val campfireContent: CampfireContentWithInsets
  val miniPlayerContent: MiniPlayerContent

  @ContributesTo(AppScope::class)
  @GraphExtension.Factory
  interface Factory {
    fun create(): WindowComponent
  }
}
