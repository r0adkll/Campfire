// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.mediaprogress

import app.campfire.core.di.UserScope
import app.campfire.data.mapping.asDomainModel
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import app.campfire.socket.events.UserItemProgressUpdated
import app.campfire.user.api.MediaProgressRepository
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@ContributesIntoSet(UserScope::class, binding = binding<SocketEventListener>())
@Inject
class MediaProgressSocketListener(
  private val mediaProgressRepository: MediaProgressRepository,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    if (event !is UserItemProgressUpdated) return
    // skipUpload: never echo a remote write back to the server. onlyIfFresher: server
    // session syncs make this device's own echoes arrive here frequently while playing;
    // last-write-wins keeps them from clobbering the fresher local row.
    mediaProgressRepository.updateProgress(
      newProgress = event.payload.data.asDomainModel(),
      skipUpload = true,
      onlyIfFresher = true,
    )
  }
}
