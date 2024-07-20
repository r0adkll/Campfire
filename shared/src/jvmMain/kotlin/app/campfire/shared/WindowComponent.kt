// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.shared

import app.campfire.core.di.ActivityScope
import app.campfire.core.di.MergeActivityScope
import app.campfire.core.di.MergeAppScope
import app.campfire.shared.di.UiComponent
import app.campfire.shared.root.CampfireContentWithInsets
import com.r0adkll.kotlininject.merge.annotations.ContributesSubcomponent

@ActivityScope
@ContributesSubcomponent(
  scope = MergeActivityScope::class,
  parentScope = MergeAppScope::class,
)
abstract class WindowComponent : UiComponent {
  abstract val campfireContent: CampfireContentWithInsets

  companion object
}
