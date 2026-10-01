// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.settings

import android.app.Application
import app.campfire.bookinfo.hardcover.di.HardcoverSettings
import app.campfire.core.di.AppScope
import app.campfire.securesettings.keystoreSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual interface PlatformHardcoverSettingsComponent {

  @SingleIn(AppScope::class)
  @Provides
  @HardcoverSettings
  fun provideHardcoverSettings(
    application: Application,
  ): Settings = keystoreSettings(
    context = application,
    name = "secure_hardcover",
    legacyName = "hardcover_prefs",
  )
}
