// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.backup

import app.campfire.account.api.BackedUpAccount
import app.campfire.account.api.TokenBackup
import app.campfire.account.server.db.ServerDao
import app.campfire.account.storage.ExtraHeaderStorage
import app.campfire.account.storage.TokenStorage
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.UserId
import app.campfire.settings.api.CampfireSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps [TokenBackup] in step with the accounts on the device, while the user wants to stay
 * signed in after a reinstall.
 */
@SingleIn(AppScope::class)
@Inject
class AccountBackups(
  private val tokenBackup: TokenBackup,
  private val tokenStorage: TokenStorage,
  private val extraHeaderStorage: ExtraHeaderStorage,
  private val serverDao: ServerDao,
  private val settings: CampfireSettings,
) {

  private val mutex = Mutex()

  private val isEnabled: Boolean
    get() = tokenBackup.isAvailable && settings.keepSignedInAfterReinstall

  /**
   * Keep the current sign-in of [userId]. It's read when this runs rather than passed in, so
   * back-to-back token rotations can't leave an older one kept.
   */
  suspend fun backUp(userId: UserId) = mutex.withLock {
    if (isEnabled) backUpLocked(userId)
  }

  suspend fun forget(userId: UserId) = mutex.withLock {
    if (tokenBackup.isAvailable) tokenBackup.remove(userId)
  }

  /**
   * Keep every account, or none when the user turned it off
   */
  suspend fun sync() = mutex.withLock {
    if (!tokenBackup.isAvailable) return@withLock
    if (settings.keepSignedInAfterReinstall) {
      serverDao.observeAll().first().forEach { backUpLocked(it.user.id) }
    } else {
      tokenBackup.clear()
    }
  }

  private suspend fun backUpLocked(userId: UserId) {
    val server = serverDao.observeAll().first().firstOrNull { it.user.id == userId } ?: return
    val token = tokenStorage.get(userId) ?: return
    tokenBackup.put(
      BackedUpAccount(
        serverUrl = server.url,
        serverName = server.name,
        userId = userId,
        token = token,
        extraHeaders = extraHeaderStorage.get(userId).orEmpty(),
      ),
    )
  }
}

/**
 * Syncs [AccountBackups] on launch, which also carries the latest backup settings (like whether it
 * may go to the cloud) to accounts kept by an older version, and whenever the user flips it.
 */
@ContributesIntoSet(AppScope::class)
@Inject
class AccountBackupInitializer(
  private val accountBackups: AccountBackups,
  private val settings: CampfireSettings,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : AppInitializer {

  override suspend fun onInitialize() {
    applicationScope.launch {
      settings.observeKeepSignedInAfterReinstall().collect {
        accountBackups.sync()
      }
    }
  }
}

/**
 * For builds without somewhere to keep sign-ins. Google Play builds replace it with Block Store.
 */
@ContributesBinding(AppScope::class)
@Inject
class NoOpTokenBackup : TokenBackup {
  override val isAvailable: Boolean = false
  override suspend fun put(account: BackedUpAccount) = Unit
  override suspend fun remove(userId: UserId) = Unit
  override suspend fun clear() = Unit
  override suspend fun getAll(): List<BackedUpAccount> = emptyList()
}
