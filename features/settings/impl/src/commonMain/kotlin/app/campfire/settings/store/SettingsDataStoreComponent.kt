// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okio.Path

/** Where the settings DataStore file lives on this platform. */
class SettingsDataStoreFile(val path: Path)

internal const val SETTINGS_DATASTORE_FILE_NAME = "settings.preferences_pb"

@ContributesTo(AppScope::class)
interface SettingsDataStoreComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettingsDataStore(
    file: SettingsDataStoreFile,
    legacySettings: ObservableSettings,
    dispatcherProvider: DispatcherProvider,
  ): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    // An unreadable file starts over, and the migration then recovers what the old storage still holds
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    migrations = listOf(LegacySettingsMigration(legacySettings)),
    scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io),
    produceFile = { file.path },
  )
}
