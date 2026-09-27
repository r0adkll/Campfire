// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.settings

import app.campfire.bookinfo.hardcover.di.HardcoverSettings
import app.campfire.core.di.AppScope
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.prefs.Preferences

actual interface PlatformHardcoverSettingsComponent {

  @SingleIn(AppScope::class)
  @Provides
  @HardcoverSettings
  fun provideHardcoverSettings(delegate: Preferences): Settings {
    return PreferencesSettings(delegate)
  }
}
