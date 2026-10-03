// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.russhwolf.settings.MapSettings
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toOkioPath

class DataStoreSettingsFileTest {

  private val directory = createTempDirectory("settings").toFile()
  private val file = File(directory, SETTINGS_DATASTORE_FILE_NAME)
  private val legacy = MapSettings()

  @AfterTest
  fun tearDown() {
    directory.deleteRecursively()
  }

  private fun open(scope: CoroutineScope): Pair<DataStore<Preferences>, DataStoreSettings> {
    val dataStore = PreferenceDataStoreFactory.createWithPath(
      migrations = listOf(LegacySettingsMigration(legacy)),
      scope = scope,
      produceFile = { file.toOkioPath() },
    )
    return dataStore to DataStoreSettings(dataStore, scope)
  }

  @Test
  fun `settings survive reopening the file`() = runBlocking {
    val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val (dataStore, first) = open(firstScope)
    first.load()
    first.putBoolean(KEY_SOCKET_ENABLED, false)
    withTimeout(5.seconds) {
      dataStore.data.first { it[booleanPreferencesKey(KEY_SOCKET_ENABLED)] == false }
    }
    // Only one DataStore may have the file open, so close the first before reopening
    firstScope.coroutineContext.job.cancelAndJoin()

    val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val (_, second) = open(secondScope)
    second.load()

    assertThat(second.getBoolean(KEY_SOCKET_ENABLED, true)).isEqualTo(false)
    secondScope.coroutineContext.job.cancelAndJoin()
  }

  @Test
  fun `the first load migrates the old settings`() = runBlocking {
    legacy.putString(KEY_THEME, "dark")
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val (_, settings) = open(scope)

    settings.load()

    assertThat(settings.getStringOrNull(KEY_THEME)).isEqualTo("dark")
    scope.coroutineContext.job.cancelAndJoin()
  }
}
