// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.ios.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.UiScope
import app.campfire.ios.CampfireUiViewControllerFactory
import dev.zacsweers.metro.ContributesGraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import platform.UIKit.UIViewController

@SingleIn(UiScope::class)
@ContributesGraphExtension(UiScope::class)
interface HomeUiControllerComponent {
  val uiViewControllerFactory: CampfireUiViewControllerFactory

  @Provides
  @SingleIn(UiScope::class)
  fun uiViewController(bind: CampfireUiViewControllerFactory): UIViewController = bind.create()

  @ContributesGraphExtension.Factory(AppScope::class)
  interface Factory {
    fun create(): HomeUiControllerComponent
  }
}
