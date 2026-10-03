// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.coroutines.DispatcherProvider
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toOkioPath

class DefaultUserSettingsStoresTest {

  private val directory = createTempDirectory("settings").toFile()
  private val appSettings = MapSettings()

  private fun stores() = DefaultUserSettingsStores(
    file = SettingsDataStoreFile(File(directory, SETTINGS_DATASTORE_FILE_NAME).toOkioPath()),
    appSettings = appSettings,
    dispatcherProvider = DispatcherProvider(
      io = Dispatchers.IO,
      databaseWrite = Dispatchers.IO,
      databaseRead = Dispatchers.IO,
      computation = Dispatchers.Default,
      main = Dispatchers.Default,
    ),
  )

  private fun fileFor(userId: String) = File(directory, "$USER_SETTINGS_DIRECTORY/$userId.preferences_pb")

  @AfterTest
  fun tearDown() {
    directory.deleteRecursively()
  }

  @Test
  fun `an account starts from the settings the app kept for everyone`() = runBlocking {
    appSettings.putString(KEY_SORT_MODE, "duration")
    appSettings.putBoolean(KEY_SHOW_TIME_IN_BOOK, false)
    val stores = stores()

    stores.load("user-1")

    val settings = stores.storeFor("user-1")
    assertThat(settings.getStringOrNull(KEY_SORT_MODE)).isEqualTo("duration")
    assertThat(settings.getBooleanOrNull(KEY_SHOW_TIME_IN_BOOK)).isEqualTo(false)
  }

  @Test
  fun `accounts keep their settings apart`() = runBlocking {
    val stores = stores()
    stores.load("user-1")
    stores.load("user-2")

    stores.storeFor("user-1").putString(KEY_SORT_MODE, "title")

    assertThat(stores.storeFor("user-2").getStringOrNull(KEY_SORT_MODE)).isNull()
  }

  @Test
  fun `the same account gets the same open store`() {
    val stores = stores()
    assertThat(stores.storeFor("user-1")).isSameInstanceAs(stores.storeFor("user-1"))
    assertThat(stores.storeFor("user-1")).isNotSameInstanceAs(stores.storeFor("user-2"))
  }

  @Test
  fun `an account's settings are saved to its own file`() = runBlocking {
    val stores = stores()
    stores.load("user-1")

    stores.storeFor("user-1").putString(KEY_SORT_MODE, "title")

    withTimeout(5.seconds) {
      while (!fileFor("user-1").exists()) delay(10)
    }
  }

  @Test
  fun `deleting an account's settings removes its file and starts it over`() = runBlocking {
    appSettings.putString(KEY_SORT_MODE, "duration")
    val stores = stores()
    stores.load("user-1")
    stores.storeFor("user-1").putString(KEY_SORT_MODE, "title")
    withTimeout(5.seconds) {
      while (!fileFor("user-1").exists()) delay(10)
    }

    stores.delete("user-1")

    assertThat(fileFor("user-1").exists()).isFalse()
    // Reopening the account's file must not trip DataStore's one-open-store-per-file check
    stores.load("user-1")
    assertThat(stores.storeFor("user-1").getStringOrNull(KEY_SORT_MODE)).isEqualTo("duration")
  }

  @Test
  fun `signed out, the settings live in memory`() = runBlocking {
    val stores = stores()
    stores.load(null)

    stores.storeFor(null).putBoolean(KEY_SHOW_TIME_IN_BOOK, false)

    assertThat(stores.storeFor(null).getBooleanOrNull(KEY_SHOW_TIME_IN_BOOK)).isEqualTo(false)
    assertThat(File(directory, USER_SETTINGS_DIRECTORY).exists()).isFalse()
  }

  @Test
  fun `an account's settings can't be read before they're loaded`() {
    assertFailure { stores().storeFor("user-1").getBooleanOrNull(KEY_SHOW_TIME_IN_BOOK) }
      .isInstanceOf<IllegalStateException>()
  }

  @Test
  fun `unsafe characters in an account id don't escape the settings directory`() = runBlocking {
    val stores = stores()
    stores.load("../user/1")
    stores.storeFor("../user/1").putString(KEY_SORT_MODE, "title")

    withTimeout(5.seconds) {
      while (!fileFor("___user_1").exists()) delay(10)
    }
    assertThat(fileFor("___user_1").exists()).isTrue()
  }
}
