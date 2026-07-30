// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.updates

import app.campfire.core.di.AppScope
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateSource
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class NoOpUpdateSource : AppUpdateSource {
  override val isSupported: Boolean = false

  override fun isSignedIn(): Boolean = true

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
