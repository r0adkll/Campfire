package app.campfire.network.envelopes

import app.campfire.network.models.Series
import kotlinx.serialization.Serializable

@Serializable
class SeriesResponse(
  val results: List<Series>,
  val total: Int,
  val limit: Int,
  val page: Int,
)
