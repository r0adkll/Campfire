// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.settings

import app.campfire.core.di.AppScope
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.prefs.Preferences

/**
 * Component to be implemented by platform configurations and
 * then used to contribute a DI component to provide the settings
 */
actual interface PlatformTokenSettingsComponent {

  @SingleIn(AppScope::class)
  @Provides
  @TokenSettings
  fun provideTokenSettings(delegate: Preferences): Settings {
    return PreferencesSettings(delegate)
  }

  @SingleIn(AppScope::class)
  @Provides
  @ExtraHeaderSettings
  fun provideExtraHeaderSettings(delegate: Preferences): Settings {
    return PreferencesSettings(delegate)
  }
}
