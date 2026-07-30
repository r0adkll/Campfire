// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

data class EpisodeDownloadQueueCleared(
  val libraryItemId: String,
) : SocketEvent {
  override fun toString(): String = "EpisodeDownloadQueueCleared(libraryItemId=$libraryItemId)"

  companion object : SocketEventConfig<EpisodeDownloadQueueCleared> {
    override val name: String = "episode_download_queue_cleared"
    override fun Json.decode(element: JsonElement): EpisodeDownloadQueueCleared {
      val libraryItemId = (element as JsonPrimitive).content
      return EpisodeDownloadQueueCleared(libraryItemId)
    }
  }
}
