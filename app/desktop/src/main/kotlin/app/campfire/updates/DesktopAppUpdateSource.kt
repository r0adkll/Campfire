// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.AppScope
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateSource
import app.campfire.updates.source.FakeAppUpdateSource
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.tatarka.inject.annotations.Inject

/**
 * Desktop has no real update distribution channel. On debug builds this delegates to
 * the developer-settings driven [FakeAppUpdateSource] so the app update widget and
 * flows can be tested from the Developer settings pane; otherwise it is a no-op.
 */
@ContributesBinding(AppScope::class)
@Inject
class DesktopAppUpdateSource(
  applicationInfo: ApplicationInfo,
  fake: FakeAppUpdateSource,
) : AppUpdateSource {

  private val delegate: AppUpdateSource? = fake.takeIf { applicationInfo.debugBuild }

  override val isSupported: Boolean
    get() = delegate?.isSupported ?: false

  override fun changes(): Flow<Unit> = delegate?.changes() ?: emptyFlow()

  override fun isSignedIn(): Boolean = delegate?.isSignedIn() ?: true

  override suspend fun signIn() {
    delegate?.signIn()
  }

  override suspend fun isUpdateAvailable(): Boolean = delegate?.isUpdateAvailable() ?: false

  override suspend fun getAvailableUpdate(): AppUpdate? = delegate?.getAvailableUpdate()

  override suspend fun installUpdate(): Flow<AppUpdateProgress> =
    delegate?.installUpdate() ?: emptyFlow()
}
