// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.deckbox.shared.di

import app.campfire.core.di.ActivityScope
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.shared.CampfireUiViewController
import com.r0adkll.kimchi.annotations.ContributesSubcomponent
import me.tatarka.inject.annotations.Provides
import platform.UIKit.UIViewController

@SingleIn(ActivityScope::class)
@ContributesSubcomponent(
  scope = ActivityScope::class,
  parentScope = AppScope::class,
)
abstract class HomeUiControllerComponent {
  abstract val uiViewControllerFactory: () -> UIViewController

  @Provides
  @SingleIn(ActivityScope::class)
  fun uiViewController(bind: CampfireUiViewController): UIViewController = bind()

  @ContributesSubcomponent.Factory
  interface Factory {
    fun createHomeUiControllerComponent(): HomeUiControllerComponent
  }
}
