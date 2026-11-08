// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.server

import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.QueueOnStop
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.sessions.api.SessionsRepository
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.timeout

@ContributesIntoSet(UserScope::class, binding = binding<Scoped>())
@Inject
class SessionStopUseCase(
  private val sessionRepository: SessionsRepository,
  private val playbackController: PlaybackController,
) : Scoped {

  @OptIn(FlowPreview::class)
  override suspend fun onDestroy() {
    // Check for any current sessions and stop them. The queue belongs to the account, so it
    // stays for when the user switches back; logging out deletes it with the account.
    val currentSession = sessionRepository.getCurrentSession()
    if (currentSession != null) {
      playbackController.stopSession(
        itemId = currentSession.libraryItem.id,
        queue = QueueOnStop.Keep,
        episodeId = currentSession.episodeId,
      )

      // Force wait until current session is null
      sessionRepository
        .observeCurrentSession()
        .filter { it == null }
        .onEmpty { emit(null) }
        .timeout(1.seconds)
        .firstOrNull()
    }
  }
}
