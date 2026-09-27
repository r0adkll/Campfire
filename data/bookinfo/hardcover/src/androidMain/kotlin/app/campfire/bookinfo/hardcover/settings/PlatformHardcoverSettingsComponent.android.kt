// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.settings

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.campfire.bookinfo.hardcover.di.HardcoverSettings
import app.campfire.core.di.AppScope
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual interface PlatformHardcoverSettingsComponent {

  @SingleIn(AppScope::class)
  @Provides
  @HardcoverSettings
  fun provideHardcoverSettings(
    application: Application,
  ): Settings {
    val masterKey = MasterKey.Builder(application)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()

    return SharedPreferencesSettings(
      delegate = EncryptedSharedPreferences.create(
        application,
        "hardcover_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
      ),
    )
  }
}
