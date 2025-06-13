// Copyright 2023, Google LLC, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.ios.di

import app.campfire.common.di.SharedAppComponent
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForAppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class, isExtendable = true)
interface IosApplicationComponent : SharedAppComponent {

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
