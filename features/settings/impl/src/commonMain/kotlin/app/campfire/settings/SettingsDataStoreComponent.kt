// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.settings.api.SettingsLoader
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okio.Path

/** Qualifies the DataStore-backed [ObservableSettings] the settings implementations read and write. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SettingsStore

/** Where the settings DataStore file lives on this platform. */
class SettingsDataStoreFile(val path: Path)

internal const val SETTINGS_DATASTORE_FILE_NAME = "settings.preferences_pb"

@ContributesTo(AppScope::class)
interface SettingsDataStoreComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideDataStoreSettings(
    file: SettingsDataStoreFile,
    legacySettings: ObservableSettings,
    dispatcherProvider: DispatcherProvider,
  ): DataStoreSettings {
    val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    val dataStore = PreferenceDataStoreFactory.createWithPath(
      // An unreadable file starts over, and the migration then recovers what the old storage still holds
      corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
      migrations = listOf(LegacySettingsMigration(legacySettings)),
      scope = scope,
      produceFile = { file.path },
    )
    return DataStoreSettings(dataStore, scope)
  }

  @SettingsStore
  @Provides
  fun provideSettingsStore(settings: DataStoreSettings): ObservableSettings = settings

  @Provides
  fun provideSettingsLoader(settings: DataStoreSettings): SettingsLoader = SettingsLoader(settings::load)
}
