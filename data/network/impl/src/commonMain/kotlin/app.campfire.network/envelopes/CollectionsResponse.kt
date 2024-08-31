package app.campfire.network.envelopes

import app.campfire.network.models.Collection
import kotlinx.serialization.Serializable

// TODO: This envelope is consistent across many endpoints and we should commonize it
@Serializable
class CollectionsResponse(
  val results: List<Collection>,
)
