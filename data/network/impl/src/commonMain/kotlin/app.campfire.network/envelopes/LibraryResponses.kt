package app.campfire.network.envelopes

import app.campfire.network.models.BookMetadata
import app.campfire.network.models.Library
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MediaType
import app.campfire.network.models.MinifiedBookMetadata
import kotlinx.serialization.Serializable

@Serializable
class AllLibrariesResponse(
  val libraries: List<Library>,
)

@Serializable
class LibraryItemsResponse(
  val results: List<LibraryItemMinified<MinifiedBookMetadata>>,
  val total: Int,
  val limit: Int,
  val page: Int,
  val sortDesc: Boolean,
  val mediaType: MediaType,
  val minified: Boolean,
  val collapseseries: Boolean,
  val include: String,
  val offset: Int,
)
