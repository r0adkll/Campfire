// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.preference.PreferenceManager
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

/**
 * Auto Backup (`android:fullBackupOnly="true"`) does the backing up and restoring, following
 * `data_extraction_rules.xml`; this agent only exists for [onRestoreFinished]. It runs in a
 * restricted process without the app's DI graph, so it edits the stored settings directly.
 */
class SettingsBackupAgent : BackupAgent() {

  override fun onRestoreFinished() {
    // Settings saved before DataStore, which the app migrates when it next starts
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    // Commit synchronously, the process can be killed as soon as this returns
    SharedPreferencesSettings(preferences, commit = true).clearDeviceBoundSettings()

    clearDeviceBoundDataStoreSettings()
  }

  private fun clearDeviceBoundDataStoreSettings() {
    val file = settingsDataStoreFile()
    if (!file.exists()) return

    // The app isn't running during a restore, so this is the only DataStore open on the file
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    try {
      val dataStore = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = { file },
      )
      runBlocking {
        dataStore.edit { settings ->
          // Keys match by name, whatever type the value was stored as
          DeviceBoundSettingKeys.forEach { settings.remove(stringPreferencesKey(it)) }
        }
      }
    } finally {
      scope.cancel()
    }
  }

  // Key/value backup is unused with fullBackupOnly
  override fun onBackup(
    oldState: ParcelFileDescriptor?,
    data: BackupDataOutput?,
    newState: ParcelFileDescriptor?,
  ) = Unit

  override fun onRestore(
    data: BackupDataInput?,
    appVersionCode: Int,
    newState: ParcelFileDescriptor?,
  ) = Unit
}
