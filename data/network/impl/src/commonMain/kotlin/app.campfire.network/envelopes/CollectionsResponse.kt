package app.campfire.network.envelopes

import app.campfire.network.models.Collection
import kotlinx.serialization.Serializable

@Serializable
class CollectionsResponse(
  val results: List<Collection>,
) : Envelope() {

  override fun applyPostage() {
    results.forEach { collection ->
      collection.origin = origin
      collection.books.forEach { book ->
        book.origin = origin
      }
    }
  }
}
