package app.campfire.network.envelopes

import app.campfire.network.models.Series
import kotlinx.serialization.Serializable

@Serializable
class SeriesResponse(
  val results: List<Series>,
  val total: Int,
  val limit: Int,
  val page: Int,
  val sortBy: String? = null,
  val sortDesc: Boolean = false,
  val filterBy: String? = null,
  val minified: Boolean = true,
)
