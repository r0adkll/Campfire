package app.campfire.data.mapping

import app.campfire.core.model.Library
import app.campfire.core.model.MediaType
import app.campfire.core.model.UserId
import app.campfire.data.Library as DbLibrary
import app.campfire.network.models.Library as NetworkLibrary
import app.campfire.network.models.MediaType as NetworkMediaType

fun NetworkLibrary.asDbModel(userId: UserId): DbLibrary {
  return DbLibrary(
    id = id,
    userId = userId,
    name = name,
    displayOrder = displayOrder,
    icon = icon,
    mediaType = mediaType.value, //
    provider = provider,
    createdAt = createdAt,
    lastUpdate = lastUpdate,
    coverAspectRatio = settings.coverAspectRatio,
    audiobooksOnly = settings.audiobooksOnly == true,
  )
}

fun DbLibrary.asDomainModel(): Library {
  return Library(
    id = id,
    name = name,
    displayOrder = displayOrder,
    icon = Library.Icon.from(icon),
    mediaType = when (NetworkMediaType.decode(mediaType)) {
      NetworkMediaType.Book -> MediaType.Book
      NetworkMediaType.Podcast,
      NetworkMediaType.Podcast2,
      -> MediaType.Podcast
      else -> MediaType.Book
    },
    provider = provider,
    coverAspectRatio = coverAspectRatio,
    audiobooksOnly = audiobooksOnly,
    createdAt = createdAt,
    lastUpdate = lastUpdate,
  )
}
