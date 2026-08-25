// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.core.app.AppInitializer
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Hooks [MediaControllerConnector] into the app startup sequence so its process-lifecycle
 * observation begins at launch, without the connector doing work at construction time.
 */
@ContributesMultibinding(AppScope::class)
@Inject
class MediaControllerConnectorInitializer(
  private val connector: MediaControllerConnector,
  private val dispatcherProvider: DispatcherProvider,
) : AppInitializer {

  override suspend fun onInitialize() = withContext(dispatcherProvider.main) {
    connector.initialize()
  }
}
