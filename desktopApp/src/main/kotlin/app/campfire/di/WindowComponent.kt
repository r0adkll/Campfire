// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.WindowScope
import app.campfire.shared.root.CampfireContentWithInsets
import com.r0adkll.kimchi.annotations.ContributesSubcomponent

@SingleIn(WindowScope::class)
@ContributesSubcomponent(
  scope = WindowScope::class,
  parentScope = AppScope::class,
)
interface WindowComponent {
  val campfireContent: CampfireContentWithInsets

  @ContributesSubcomponent.Factory
  interface Factory {
    fun create(): WindowComponent
  }
}
