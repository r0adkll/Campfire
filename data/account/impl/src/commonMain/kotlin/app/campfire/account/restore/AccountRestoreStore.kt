// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.restore

import app.campfire.account.api.RestorableAccount
import app.campfire.account.api.RestorableAccountRepository
import app.campfire.account.server.db.ServerDao
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.UserId
import app.campfire.settings.api.DeviceSettings
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Keeps a [RestoreSnapshot] of the signed-in accounts in the app's preferences, which the platform
 * backs up (see `data_extraction_rules.xml` on Android) while the database is not. After a
 * reinstall, the accounts from the snapshot are offered on the login screen.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AccountRestoreStore(
  private val settings: ObservableSettings,
  private val serverDao: ServerDao,
  private val deviceSettings: DeviceSettings,
  private val dispatcherProvider: DispatcherProvider,
) : RestorableAccountRepository {

  private val snapshot = MutableStateFlow<RestoreSnapshot?>(null)
  private val mutex = Mutex()

  override fun observeRestorableAccounts(): Flow<List<RestorableAccount>> {
    return snapshot
      .filterNotNull()
      .map { it.restorableAccounts() }
      .distinctUntilChanged()
  }

  override suspend fun dismiss(account: RestorableAccount) {
    update { it.dismiss(account) }
  }

  /**
   * Drop [userId] from the snapshot. Must be called before the account is deleted from the
   * database, otherwise [mirror] treats it as restorable.
   */
  suspend fun forget(userId: UserId) {
    update { it.forget(userId) }
  }

  /**
   * Mirror the database's accounts into the snapshot until cancelled
   */
  suspend fun mirror() {
    combine(
      serverDao.observeAll(),
      deviceSettings.observeCurrentUserId(),
    ) { servers, currentUserId ->
      servers.map(::RestoreEntry) to currentUserId
    }.collect { (accounts, currentUserId) ->
      update { it.reconcile(accounts, currentUserId) }
    }
  }

  private suspend fun update(transform: (RestoreSnapshot) -> RestoreSnapshot) = mutex.withLock {
    val current = snapshot.value ?: withContext(dispatcherProvider.io) { load() }
    val updated = transform(current)
    if (updated != current) {
      withContext(dispatcherProvider.io) {
        settings.putString(KEY_SNAPSHOT, json.encodeToString(updated))
      }
    }
    snapshot.value = updated
  }

  private fun load(): RestoreSnapshot {
    val raw = settings.getStringOrNull(KEY_SNAPSHOT) ?: return RestoreSnapshot()
    return runCatching { json.decodeFromString<RestoreSnapshot>(raw) }
      .onFailure { bark(LogPriority.WARN, throwable = it) { "Discarding unreadable account restore snapshot" } }
      .getOrDefault(RestoreSnapshot())
  }

  private companion object {
    const val KEY_SNAPSHOT = "account_restore_snapshot"

    val json = Json { ignoreUnknownKeys = true }
  }
}
