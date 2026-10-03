// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.util.prefs.Preferences
import okio.Path.Companion.toOkioPath

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettings(delegate: Preferences): ObservableSettings = PreferencesSettings(delegate)

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettingsDataStoreFile(): SettingsDataStoreFile {
    val userRoot = System.getProperty("java.util.prefs.userRoot", System.getProperty("user.home"))
    val directory = File(userRoot, ".config/Campfire/datastore")
    return SettingsDataStoreFile(File(directory, SETTINGS_DATASTORE_FILE_NAME).toOkioPath())
  }
}
