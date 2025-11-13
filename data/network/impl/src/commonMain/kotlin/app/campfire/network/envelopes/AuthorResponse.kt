package app.campfire.network.envelopes

import app.campfire.network.models.Author
import kotlinx.serialization.Serializable

@Serializable
data class AuthorResponse(
  val authors: List<Author>,
)
