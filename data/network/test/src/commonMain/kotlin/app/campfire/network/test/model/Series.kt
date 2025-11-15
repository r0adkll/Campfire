package app.campfire.network.test.model

import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.models.Series

fun createNetworkSeries(
  id: String,
  name: String = "Network Series: $id",
  description: String? = null,
  addedAt: Long = 0L,
  updatedAt: Long = 0L,
  books: List<LibraryItemMinified<MinifiedBookMetadata>>? = null,
) = Series(
  id = id,
  name = name,
  description = description,
  addedAt = addedAt,
  updatedAt = updatedAt,
  books = books,
)
