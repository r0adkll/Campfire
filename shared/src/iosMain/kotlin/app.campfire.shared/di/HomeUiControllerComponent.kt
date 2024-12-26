// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.deckbox.shared.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.WindowScope
import app.campfire.shared.CampfireUiViewController
import com.r0adkll.kimchi.annotations.ContributesSubcomponent
import me.tatarka.inject.annotations.Provides
import platform.UIKit.UIViewController

@SingleIn(WindowScope::class)
@ContributesSubcomponent(
  scope = WindowScope::class,
  parentScope = AppScope::class,
)
interface HomeUiControllerComponent {
  val uiViewControllerFactory: () -> UIViewController

  @Provides
  @SingleIn(WindowScope::class)
  fun uiViewController(bind: CampfireUiViewController): UIViewController = bind()

  @ContributesSubcomponent.Factory
  interface Factory {
    fun createHomeUiControllerComponent(): HomeUiControllerComponent
  }
}
