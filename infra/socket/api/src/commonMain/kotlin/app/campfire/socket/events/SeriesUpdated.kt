// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.models.Series
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class SeriesUpdated(
  val series: Series,
) : SocketEvent {
  override fun toString(): String = "SeriesUpdated(id=${series.id}, name=${series.name})"

  companion object : SocketEventConfig<SeriesUpdated> {
    override val name: String = "series_updated"
    override fun Json.decode(element: JsonElement): SeriesUpdated {
      return SeriesUpdated(decodeFromJsonElement(Series.serializer(), element))
    }
  }
}
