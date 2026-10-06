// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.prefs.Preferences

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  @ForScope(AppScope::class)
  fun provideSettings(delegate: Preferences): ObservableSettings = PreferencesSettings(delegate)
}
