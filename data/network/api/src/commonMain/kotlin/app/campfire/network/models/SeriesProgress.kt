package app.campfire.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The user's progress of a series.
 *
 * @param libraryItemIds The IDs of the library items in the series.
 * @param libraryItemIdsFinished The IDs of the library items in the series that are finished.
 * @param isFinished Whether the series is finished.
 */
@Serializable
data class SeriesProgress(

  /* The IDs of the library items in the series. */
  @SerialName(value = "libraryItemIds")
  val libraryItemIds: List<String>? = null,

  /* The IDs of the library items in the series that are finished. */
  @SerialName(value = "libraryItemIdsFinished")
  val libraryItemIdsFinished: List<String>? = null,

  /* Whether the series is finished. */
  @SerialName(value = "isFinished")
  val isFinished: Boolean? = null,

)
