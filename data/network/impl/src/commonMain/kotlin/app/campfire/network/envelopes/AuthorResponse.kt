package app.campfire.network.envelopes

import app.campfire.network.models.Author
import app.campfire.network.models.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class AuthorResponse(
  val results: List<Author>,
  val total: Int,
  val limit: Int,
  val page: Int,
)
