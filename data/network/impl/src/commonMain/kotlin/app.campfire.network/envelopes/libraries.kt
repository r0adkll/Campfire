package app.campfire.network.envelopes

import app.campfire.network.models.Library
import kotlinx.serialization.Serializable

@Serializable
class AllLibrariesResponse(
  val libraries: List<Library>
)
