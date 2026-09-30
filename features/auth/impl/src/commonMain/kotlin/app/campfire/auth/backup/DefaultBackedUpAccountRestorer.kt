// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.backup

import app.campfire.account.api.AccountManager
import app.campfire.account.api.BackedUpAccountRestorer
import app.campfire.account.api.RestorableAccountRepository
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.TokenBackup
import app.campfire.auth.api.AuthException
import app.campfire.auth.api.AuthRepository
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.UserId
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultBackedUpAccountRestorer(
  private val tokenBackup: TokenBackup,
  private val authRepository: AuthRepository,
  private val serverRepository: ServerRepository,
  private val accountManager: AccountManager,
  private val restorableAccountRepository: RestorableAccountRepository,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : BackedUpAccountRestorer {

  private val restoringUsers = MutableStateFlow<Set<UserId>>(emptySet())
  override val restoring: StateFlow<Set<UserId>> = restoringUsers.asStateFlow()

  private var job: Job? = null

  override fun restore() {
    if (job != null) return
    job = applicationScope.launch { restoreAll() }
  }

  internal suspend fun restoreAll() {
    if (!tokenBackup.isAvailable) return

    val signedIn = serverRepository.getAllServers().mapTo(mutableSetOf()) { it.user.id }
    val kept = tokenBackup.getAll().filter { it.userId !in signedIn }
    if (kept.isEmpty()) return

    // Restorable accounts come most recently used first; ones without a card go last
    val order = restorableAccountRepository.observeRestorableAccounts().first().map { it.userId }
    val accounts = kept.sortedBy { account ->
      order.indexOf(account.userId).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }

    restoringUsers.value = accounts.mapTo(mutableSetOf()) { it.userId }
    var toActivate: UserId? = null
    for (account in accounts) {
      // Switching accounts rebuilds the app's graph, so hold that until they're all back
      authRepository.restore(account, activate = false)
        .onSuccess { if (toActivate == null) toActivate = account.userId }
        .onFailure { e ->
          bark(LogPriority.WARN, throwable = e) { "Couldn't restore a kept account" }
          // The server won't take this token again; a network failure might be gone next launch
          if (e is AuthException.InvalidCredentials) tokenBackup.remove(account.userId)
        }
      restoringUsers.update { it - account.userId }
    }

    toActivate?.let { userId ->
      serverRepository.getAllServers()
        .firstOrNull { it.user.id == userId }
        ?.let { accountManager.switchAccount(it.user) }
    }
  }
}
