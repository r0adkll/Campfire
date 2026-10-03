// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.UserScope
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.settings.api.UserSettingsStores
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import okio.Path

/** Qualifies the [ObservableSettings] of the signed-in account, in the user graph. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UserSettingsStore

/**
 * Keeps one open settings store per account for the life of the process. DataStore allows one open store per
 * file, so the user graph, which is rebuilt on every session change, borrows the store from here rather than
 * opening its own.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<UserSettingsStores>())
@Inject
class DefaultUserSettingsStores(
  file: SettingsDataStoreFile,
  @SettingsStore private val appSettings: ObservableSettings,
  private val dispatcherProvider: DispatcherProvider,
) : UserSettingsStores, SynchronizedObject() {

  private class OpenStore(val settings: DataStoreSettings, val scope: CoroutineScope)

  private val directory: Path = checkNotNull(file.path.parent) / USER_SETTINGS_DIRECTORY
  private val stores = mutableMapOf<UserId, OpenStore>()
  private val signedOut = DataStoreSettings(InMemoryDataStore(), newScope())

  /** The settings for [userId], or the signed-out settings when it's null. They must be loaded before use. */
  fun storeFor(userId: UserId?): DataStoreSettings {
    if (userId == null) return signedOut
    return synchronized(this) { stores.getOrPut(userId) { open(userId) } }.settings
  }

  override fun startLoading(userId: UserId) = storeFor(userId).startLoading()

  override suspend fun load(userId: UserId?) = storeFor(userId).load()

  override suspend fun delete(userId: UserId) {
    val store = synchronized(this) { stores.remove(userId) }
    // Close the store before deleting its file, so nothing writes it back
    store?.scope?.coroutineContext?.job?.cancelAndJoin()
    SystemFileSystem.delete(fileFor(userId), mustExist = false)
  }

  private fun open(userId: UserId): OpenStore {
    val scope = newScope()
    val dataStore = PreferenceDataStoreFactory.createWithPath(
      corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
      migrations = listOf(SeedLibraryViewSettingsMigration(appSettings)),
      scope = scope,
      produceFile = { fileFor(userId) },
    )
    return OpenStore(DataStoreSettings(dataStore, scope), scope)
  }

  private fun newScope() = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

  private fun fileFor(userId: UserId): Path =
    directory / "${userId.replace(UNSAFE_FILE_NAME_CHARACTERS, "_")}.preferences_pb"

  private companion object {
    val UNSAFE_FILE_NAME_CHARACTERS = Regex("[^A-Za-z0-9_-]")
  }
}

@ContributesTo(UserScope::class)
interface UserSettingsStoreComponent {

  @UserSettingsStore
  @Provides
  fun provideUserSettingsStore(
    userSession: UserSession,
    stores: DefaultUserSettingsStores,
  ): ObservableSettings = stores.storeFor(userSession.userId)
}

/** The account settings directory, next to the app's settings file. */
internal const val USER_SETTINGS_DIRECTORY = "users"

/** Stands in for a file while signed out: there's no account to keep the settings for. */
private class InMemoryDataStore : DataStore<Preferences> {
  override val data = MutableStateFlow(emptyPreferences())

  override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
    transform(data.value).also { data.value = it }
}
