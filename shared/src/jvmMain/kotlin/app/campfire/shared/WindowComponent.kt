// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.shared

import app.campfire.core.di.ActivityScope
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.shared.di.UiComponent
import app.campfire.shared.root.CampfireContentWithInsets
import com.r0adkll.kotlininject.merge.annotations.ContributesSubcomponent

@SingleIn(ActivityScope::class)
@ContributesSubcomponent(
  scope = ActivityScope::class,
  parentScope = AppScope::class,
)
abstract class WindowComponent : UiComponent {
  abstract val campfireContent: CampfireContentWithInsets

  companion object
}
