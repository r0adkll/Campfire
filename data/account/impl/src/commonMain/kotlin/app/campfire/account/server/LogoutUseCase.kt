// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.server

import app.campfire.account.api.ServerRepository
import app.campfire.account.restore.AccountRestoreStore
import app.campfire.core.di.AppScope
import app.campfire.core.model.Server
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

interface LogoutUseCase {

  suspend fun execute(server: Server)
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultLogoutUseCase(
  private val serverRepository: ServerRepository,
  private val accountRestoreStore: AccountRestoreStore,
) : LogoutUseCase {

  override suspend fun execute(server: Server) {
    // Forget the account first, or deleting it would look like a restore to the mirror
    accountRestoreStore.forget(server.user.id)

    // Delete the core server db models and relations
    serverRepository.remove(server)
  }
}
