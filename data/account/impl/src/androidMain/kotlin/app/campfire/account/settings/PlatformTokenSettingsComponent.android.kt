// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.settings

import android.app.Application
import app.campfire.core.di.AppScope
import app.campfire.securesettings.keystoreSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Component to be implemented by platform configurations and
 * then used to contribute a DI component to provide the settings
 */
actual interface PlatformTokenSettingsComponent {

  @SingleIn(AppScope::class)
  @Provides
  @TokenSettings
  fun provideTokenSettings(
    application: Application,
  ): Settings = keystoreSettings(
    context = application,
    name = "secure_tokens",
    legacyName = "token_shared_prefs",
  )

  @SingleIn(AppScope::class)
  @Provides
  @ExtraHeaderSettings
  fun provideExtraHeaderSettings(
    application: Application,
  ): Settings = keystoreSettings(
    context = application,
    name = "secure_extra_headers",
    legacyName = "extra_headers",
  )
}
