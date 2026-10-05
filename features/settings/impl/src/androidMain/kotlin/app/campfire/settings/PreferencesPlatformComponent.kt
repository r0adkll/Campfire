// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import android.app.Application
import android.content.Context
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import com.r0adkll.flatprefs.FlatPreferencesStore
import com.r0adkll.flatprefs.SharedPreferencesMigration
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettings(
    context: Application,
    dispatcherProvider: DispatcherProvider,
  ): ObservableSettings {
    val store = context.openSettingsStore()
    // Loads, and migrates on the first launch, off the thread that first reads a setting
    store.preload()
    return FlatPreferencesSettings(store, CoroutineScope(SupervisorJob() + dispatcherProvider.computation))
  }
}

/**
 * The settings store, in `files/flatprefs/settings.fpb`. Its first load moves over the settings
 * kept in the default SharedPreferences before FlatPrefs.
 */
internal fun Context.openSettingsStore(): FlatPreferencesStore = openSettingsStore(
  file = filesDir.toOkioPath() / "flatprefs" / SETTINGS_STORE_FILE_NAME,
  migrations = listOf(
    // The file PreferenceManager.getDefaultSharedPreferences() opens
    DoubleBitsMigration(SharedPreferencesMigration(this, "${packageName}_preferences")),
  ),
)
