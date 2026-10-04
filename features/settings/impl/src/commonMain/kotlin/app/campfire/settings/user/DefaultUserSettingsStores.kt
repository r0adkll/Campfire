// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.UserId
import app.campfire.settings.api.UserSettingsStores
import app.campfire.settings.store.SettingsDataStoreFile
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import okio.FileSystem
import okio.Path

/**
 * Keeps one open settings store per account for the life of the process. DataStore allows one open store per
 * file, so the user graph, which is rebuilt on every session change, borrows its store from here rather than
 * opening its own. Signed out, an in-memory store stands in: there's no account to keep the settings for.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<UserSettingsStores>())
@Inject
class DefaultUserSettingsStores(
  file: SettingsDataStoreFile,
  private val appDataStore: DataStore<Preferences>,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) applicationScope: CoroutineScope,
) : UserSettingsStores, SynchronizedObject() {

  private class OpenStore(val store: SettingsStore, val scope: CoroutineScope)

  internal var fileSystem: FileSystem = SystemFileSystem

  private val directory: Path = checkNotNull(file.path.parent) / USER_SETTINGS_DIRECTORY
  private val stores = mutableMapOf<UserId, OpenStore>()
  private val signedOut = SettingsStore(InMemoryDataStore(), applicationScope)

  /** The settings for [userId], or the signed-out settings when it's null. */
  fun storeFor(userId: UserId?): SettingsStore {
    if (userId == null) return signedOut
    return synchronized(this) { stores.getOrPut(userId) { open(userId) } }.store
  }

  override suspend fun delete(userId: UserId) {
    val store = synchronized(this) { stores.remove(userId) }
    // Close the store before deleting its file, so nothing writes it back
    store?.scope?.coroutineContext?.job?.cancelAndJoin()
    fileSystem.delete(fileFor(userId), mustExist = false)
  }

  private fun open(userId: UserId): OpenStore {
    val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    val dataStore = PreferenceDataStoreFactory.createWithPath(
      corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
      migrations = listOf(SeedUserSettingsMigration(appDataStore)),
      scope = scope,
      produceFile = { fileFor(userId) },
    )
    return OpenStore(SettingsStore(dataStore, scope), scope)
  }

  private fun fileFor(userId: UserId): Path =
    directory / "${userId.replace(UNSAFE_FILE_NAME_CHARACTERS, "_")}.preferences_pb"

  private companion object {
    val UNSAFE_FILE_NAME_CHARACTERS = Regex("[^A-Za-z0-9_-]")
  }
}

/** The account settings directory, next to the app's settings file. */
internal const val USER_SETTINGS_DIRECTORY = "users"

private class InMemoryDataStore : DataStore<Preferences> {
  override val data = MutableStateFlow(emptyPreferences())

  override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
    transform(data.value).toPreferences().also { data.value = it }
}
