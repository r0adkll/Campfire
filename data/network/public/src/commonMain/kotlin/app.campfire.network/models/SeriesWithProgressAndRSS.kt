/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
  "ArrayInDataClass",
  "EnumEntryName",
  "RemoveRedundantQualifierName",
  "UnusedImport",
)

package app.campfire.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A series object which includes the name and progress of the series.
 *
 * @param id The ID of the series.
 * @param name The name of the series.
 * @param description A description for the series. Will be null if there is none.
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 * @param updatedAt The time (in ms since POSIX epoch) when last updated.
 * @param progress
 * @param rssFeed The RSS feed for the series.
 */
@Serializable
data class SeriesWithProgressAndRSS(

  /* The ID of the series. */
  @SerialName(value = "id")
  val id: String,

  /* The name of the series. */
  @SerialName(value = "name")
  val name: kotlin.String? = null,

  /* A description for the series. Will be null if there is none. */
  @SerialName(value = "description")
  val description: kotlin.String? = null,

  /* The time (in ms since POSIX epoch) when added to the server. */
  @SerialName(value = "addedAt")
  val addedAt: kotlin.Int? = null,

  /* The time (in ms since POSIX epoch) when last updated. */
  @SerialName(value = "updatedAt")
  val updatedAt: kotlin.Int? = null,

  @SerialName(value = "progress")
  val progress: SeriesProgress? = null,

  /* The RSS feed for the series. */
  @SerialName(value = "rssFeed")
  val rssFeed: kotlin.String? = null,

)
