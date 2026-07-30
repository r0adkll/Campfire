// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.models.Series
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class SeriesAdded(
  val series: Series,
) : SocketEvent {
  override fun toString(): String = "SeriesAdded(id=${series.id}, name=${series.name})"

  companion object : SocketEventConfig<SeriesAdded> {
    override val name: String = "series_added"
    override fun Json.decode(element: JsonElement): SeriesAdded {
      return SeriesAdded(decodeFromJsonElement(Series.serializer(), element))
    }
  }
}
