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
