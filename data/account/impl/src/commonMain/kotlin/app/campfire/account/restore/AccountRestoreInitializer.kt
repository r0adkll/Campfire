// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.restore

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ContributesIntoSet(AppScope::class)
@Inject
class AccountRestoreInitializer(
  private val accountRestoreStore: AccountRestoreStore,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : AppInitializer {

  override suspend fun onInitialize() {
    // Mirroring never completes, so it can't hold up the other initializers
    applicationScope.launch {
      accountRestoreStore.mirror()
    }
  }
}
