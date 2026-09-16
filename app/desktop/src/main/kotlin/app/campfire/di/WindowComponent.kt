// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.di

import app.campfire.common.root.CampfireContentWithInsets
import app.campfire.common.root.MiniPlayerContent
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UiScope
import com.r0adkll.kimchi.annotations.ContributesSubcomponent

@SingleIn(UiScope::class)
@ContributesSubcomponent(
  scope = UiScope::class,
  parentScope = AppScope::class,
)
interface WindowComponent {
  val campfireContent: CampfireContentWithInsets
  val miniPlayerContent: MiniPlayerContent

  @ContributesSubcomponent.Factory
  interface Factory {
    fun create(): WindowComponent
  }
}
