// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.settings.ContentSortMode
import app.campfire.settings.library.KEY_SHOW_TIME_IN_BOOK
import app.campfire.settings.library.KEY_SORT_MODE
import app.campfire.settings.library.LibraryViewSettingsImpl
import app.campfire.settings.store.InMemoryPreferencesDataStore
import app.campfire.settings.store.SETTINGS_DATASTORE_FILE_NAME
import app.campfire.settings.store.SettingsDataStoreFile
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class DefaultUserSettingsStoresTest {

  private val directory = createTempDirectory("settings").toFile()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val app = InMemoryPreferencesDataStore(
    mutablePreferencesOf(
      stringPreferencesKey(KEY_SORT_MODE) to ContentSortMode.Duration.storageKey,
      booleanPreferencesKey(KEY_SHOW_TIME_IN_BOOK) to false,
    ),
  )

  private fun stores() = DefaultUserSettingsStores(
    file = SettingsDataStoreFile(File(directory, SETTINGS_DATASTORE_FILE_NAME).toOkioPath()),
    appDataStore = app,
    dispatcherProvider = DispatcherProvider(
      io = Dispatchers.IO,
      databaseWrite = Dispatchers.IO,
      databaseRead = Dispatchers.IO,
      computation = Dispatchers.Default,
      main = Dispatchers.Default,
    ),
    applicationScope = scope,
  )

  private fun DefaultUserSettingsStores.libraryView(userId: String?) = LibraryViewSettingsImpl(storeFor(userId))

  private fun fileFor(userId: String) = File(directory, "$USER_SETTINGS_DIRECTORY/$userId.preferences_pb")

  @AfterTest
  fun tearDown() {
    scope.cancel()
    directory.deleteRecursively()
  }

  @Test
  fun `an account starts from the settings the app kept for everyone`() = runBlocking {
    val settings = stores().libraryView("user-1")

    assertThat(settings.observeLibrarySortMode().first()).isEqualTo(ContentSortMode.Duration)
    assertThat(settings.observeShowTimeInBook().first()).isFalse()
  }

  @Test
  fun `accounts keep their settings apart`() = runBlocking {
    val stores = stores()

    stores.libraryView("user-1").setLibrarySortMode(ContentSortMode.Title)

    assertThat(stores.libraryView("user-1").observeLibrarySortMode().first()).isEqualTo(ContentSortMode.Title)
    assertThat(stores.libraryView("user-2").observeLibrarySortMode().first()).isEqualTo(ContentSortMode.Duration)
  }

  @Test
  fun `the same account gets the same open store`() {
    val stores = stores()
    assertThat(stores.storeFor("user-1")).isSameInstanceAs(stores.storeFor("user-1"))
    assertThat(stores.storeFor("user-1")).isNotSameInstanceAs(stores.storeFor("user-2"))
  }

  @Test
  fun `an account's settings are saved to its own file`() = runBlocking {
    val settings = stores().libraryView("user-1")

    settings.setLibrarySortMode(ContentSortMode.Title)
    // A read waits for the write before it, so it's on disk once this returns
    settings.observeLibrarySortMode().first()

    assertThat(fileFor("user-1").exists()).isTrue()
  }

  @Test
  fun `deleting an account's settings removes its file and starts it over`() = runBlocking {
    val stores = stores()
    stores.libraryView("user-1").setLibrarySortMode(ContentSortMode.Title)
    stores.libraryView("user-1").observeLibrarySortMode().first()

    stores.delete("user-1")

    assertThat(fileFor("user-1").exists()).isFalse()
    // Reopening the account's file must not trip DataStore's one-open-store-per-file check
    assertThat(stores.libraryView("user-1").observeLibrarySortMode().first()).isEqualTo(ContentSortMode.Duration)
  }

  @Test
  fun `signed out, the settings live in memory`() = runBlocking {
    val stores = stores()

    stores.libraryView(null).setShowTimeInBook(true)

    assertThat(stores.libraryView(null).observeShowTimeInBook().first()).isTrue()
    assertThat(File(directory, USER_SETTINGS_DIRECTORY).exists()).isFalse()
  }

  @Test
  fun `unsafe characters in an account id don't escape the settings directory`() = runBlocking {
    val settings = stores().libraryView("../user/1")

    settings.setLibrarySortMode(ContentSortMode.Title)
    settings.observeLibrarySortMode().first()

    assertThat(fileFor("___user_1").exists()).isTrue()
  }
}
