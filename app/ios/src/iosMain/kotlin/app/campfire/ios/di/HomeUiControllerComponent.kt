// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ios.di

import app.campfire.core.di.UiScope
import app.campfire.ios.CampfireUiViewController
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import platform.UIKit.UIViewController

@GraphExtension(UiScope::class)
interface HomeUiControllerComponent {
  val uiViewControllerFactory: () -> UIViewController

  @Provides
  @SingleIn(UiScope::class)
  fun uiViewController(campfireUiViewController: CampfireUiViewController): UIViewController =
    campfireUiViewController()

  // Implemented directly by IosApplicationComponent, so Swift sees the factory function.
  @GraphExtension.Factory
  interface Factory {
    fun createHomeUiControllerComponent(): HomeUiControllerComponent
  }
}
