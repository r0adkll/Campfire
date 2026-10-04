// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.app.ConnectionSettingsImpl
import app.campfire.settings.theme.KEY_THEME
import app.campfire.settings.theme.ThemeSettingsImpl
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.russhwolf.settings.MapSettings
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class SettingsDataStoreFileTest {

  private val directory = createTempDirectory("settings").toFile()
  private val file = File(directory, SETTINGS_DATASTORE_FILE_NAME)
  private val legacy = MapSettings()

  @AfterTest
  fun tearDown() {
    directory.deleteRecursively()
  }

  /** Opens the file the way the app does; only one may be open at a time, so cancel [scope] before reopening. */
  private fun open(scope: CoroutineScope): SettingsStore {
    val dataStore = PreferenceDataStoreFactory.createWithPath(
      migrations = listOf(LegacySettingsMigration(legacy)),
      scope = scope,
      produceFile = { file.toOkioPath() },
    )
    return SettingsStore(dataStore, scope)
  }

  @Test
  fun `settings survive reopening the file`() = runBlocking {
    val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val first = ConnectionSettingsImpl(open(firstScope))
    first.setSocketEnabled(false)
    // A read waits for the write before it, so the value is on disk once this returns
    assertThat(first.observeSocketEnabled().first()).isFalse()
    firstScope.coroutineContext.job.cancelAndJoin()

    val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val second = ConnectionSettingsImpl(open(secondScope))

    assertThat(second.observeSocketEnabled().first()).isFalse()
    secondScope.coroutineContext.job.cancelAndJoin()
  }

  @Test
  fun `the first read migrates the old settings`() = runBlocking {
    legacy.putString(KEY_THEME, ThemeMode.DARK.storageKey)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val theme = ThemeSettingsImpl(open(scope), legacy)

    assertThat(theme.observeTheme().first()).isEqualTo(ThemeMode.DARK)
    scope.coroutineContext.job.cancelAndJoin()
  }

  @Test
  fun `the theme mode is mirrored for the first frame`() = runBlocking {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val theme = ThemeSettingsImpl(open(scope), legacy)

    theme.setThemeMode(ThemeMode.LIGHT)

    assertThat(theme.lastThemeMode()).isEqualTo(ThemeMode.LIGHT)
    assertThat(theme.observeTheme().first()).isEqualTo(ThemeMode.LIGHT)
    scope.coroutineContext.job.cancelAndJoin()
  }
}
