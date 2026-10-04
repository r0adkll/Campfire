// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import app.campfire.core.di.AppScope
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettings(delegate: AppSharedPreferences): ObservableSettings {
    return SharedPreferencesSettings(delegate)
  }

  @SingleIn(AppScope::class)
  @Provides
  fun provideAppPreferences(
    context: Application,
  ): AppSharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)
}

typealias AppSharedPreferences = SharedPreferences
