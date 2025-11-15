package app.campfire.data.mapping

import app.campfire.core.model.Media
import app.campfire.data.MetadataAuthor
import app.campfire.network.models.AuthorSeries

fun AuthorSeries.asDbModel(mediaId: String): MetadataAuthor {
  return MetadataAuthor(
    id = id,
    name = name,
    mediaId = mediaId,
  )
}

fun Media.AuthorMetadata.asDbModel(mediaId: String): MetadataAuthor {
  return MetadataAuthor(
    id = id,
    name = name,
    mediaId = mediaId,
  )
}
