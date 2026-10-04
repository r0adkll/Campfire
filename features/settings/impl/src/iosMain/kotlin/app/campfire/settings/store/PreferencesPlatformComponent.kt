// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import app.campfire.core.di.AppScope
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettings(delegate: NSUserDefaults): ObservableSettings =
    NSUserDefaultsSettings(delegate)

  @OptIn(ExperimentalForeignApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideSettingsDataStoreFile(): SettingsDataStoreFile {
    val applicationSupport = NSFileManager.defaultManager.URLForDirectory(
      directory = NSApplicationSupportDirectory,
      inDomain = NSUserDomainMask,
      appropriateForURL = null,
      create = true,
      error = null,
    )
    val directory = requireNotNull(applicationSupport?.path) { "Application Support is unavailable" }
    return SettingsDataStoreFile("$directory/datastore/$SETTINGS_DATASTORE_FILE_NAME".toPath())
  }
}
