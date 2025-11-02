package app.campfire.data.mapping

import app.campfire.core.model.MediaType
import app.campfire.network.models.MediaType as NetworkMediaType

fun NetworkMediaType.asDomainModel(): MediaType = when (this) {
  NetworkMediaType.Book -> MediaType.Book
  NetworkMediaType.Podcast -> MediaType.Podcast
  NetworkMediaType.Podcast2 -> MediaType.Podcast
}

fun MediaType.asNetworkModel(): NetworkMediaType = when (this) {
  MediaType.Book -> NetworkMediaType.Book
  MediaType.Podcast -> NetworkMediaType.Podcast2
}
