// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ios.di

import app.campfire.common.di.SharedAppComponent
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import kotlin.experimental.ExperimentalNativeApi
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice

@DependencyGraph(AppScope::class)
interface IosApplicationComponent :
  SharedAppComponent,
  HomeUiControllerComponent.Factory {

  @OptIn(ExperimentalNativeApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideApplicationId(): app.campfire.core.app.ApplicationInfo = app.campfire.core.app.ApplicationInfo(
    packageName = NSBundle.mainBundle.bundleIdentifier ?: "app.campfire",
    debugBuild = Platform.isDebugBinary,
    flavor = app.campfire.core.app.Flavor.Standard,
    versionName = NSBundle.mainBundle.infoDictionary
      ?.get("CFBundleShortVersionString") as? String
      ?: "",
    versionCode = (
      NSBundle.mainBundle.infoDictionary
        ?.get("CFBundleVersion") as? String
      )
      ?.toIntOrNull()
      ?: 0,
    osName = UIDevice.currentDevice.systemName,
    osVersion = UIDevice.currentDevice.systemVersion,
  )

  @Provides
  fun provideNsUserDefaults(): NSUserDefaults = NSUserDefaults.standardUserDefaults
}

/** Creates the application graph; called from Swift (`iOSApp.swift`). */
fun createIosApplicationComponent(): IosApplicationComponent = createGraph<IosApplicationComponent>()
