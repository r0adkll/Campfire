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
    "UnusedImport"
)

package app.campfire.network.models

import app.campfire.network.models.PodcastEpisode

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 *
 *
 * @param episodes
 */
@Serializable

data class CheckNewEpisodes200Response (

    @SerialName(value = "episodes")
    val episodes: kotlin.collections.List<app.campfire.network.models.PodcastEpisode>? = null

) {


}

