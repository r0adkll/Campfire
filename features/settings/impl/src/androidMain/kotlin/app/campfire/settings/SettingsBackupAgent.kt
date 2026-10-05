// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor

/**
 * Auto Backup (`android:fullBackupOnly="true"`) does the backing up and restoring, following
 * `data_extraction_rules.xml`; this agent only exists for [onRestoreFinished]. It runs in a
 * restricted process without the app's DI graph, so it opens the settings store directly.
 */
class SettingsBackupAgent : BackupAgent() {

  override fun onRestoreFinished() {
    // Opening the store first migrates settings restored from a backup made before FlatPrefs, so
    // both kinds of backup are cleared here. Commit synchronously, the process can be killed as soon
    // as this returns.
    openSettingsStore().commitBlocking { it.clearDeviceBoundSettings() }
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
