// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.updates

import app.campfire.core.di.AppScope
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateSource
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@ContributesBinding(AppScope::class)
@Inject
class NoOpUpdateSource : AppUpdateSource {
  override val isSupported: Boolean = false

  override suspend fun isSignedIn(): Boolean = true

  override suspend fun signIn() {
  }

  override suspend fun isUpdateAvailable(): Boolean = false

  override suspend fun getAvailableUpdate(): AppUpdate? {
    return null
  }

  override suspend fun installUpdate(): Flow<AppUpdateProgress> {
    return emptyFlow()
  }
}
