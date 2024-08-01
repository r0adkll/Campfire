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

import app.campfire.network.models.SeriesBooks

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param results 
 * @param total The total number of items in the response.
 * @param limit The number of items to return. If 0, no items are returned.
 * @param page The page number (zero indexed) to return. If no limit is specified, then page will have no effect.
 * @param sortBy The field to sort by from the request.
 * @param sortDesc Whether to sort in descending order.
 * @param filterBy The field to filter by from the request. TODO
 * @param minified Return minified items if true.
 * @param include The fields to include in the response. The only current option is `rssfeed`.
 */
@Serializable

data class GetLibrarySeries200Response (

    @SerialName(value = "results")
    val results: kotlin.collections.List<SeriesBooks>? = null,

    /* The total number of items in the response. */
    @SerialName(value = "total")
    val total: kotlin.Int? = null,

    /* The number of items to return. If 0, no items are returned. */
    @SerialName(value = "limit")
    val limit: kotlin.Int? = 0,

    /* The page number (zero indexed) to return. If no limit is specified, then page will have no effect. */
    @SerialName(value = "page")
    val page: kotlin.Int? = 0,

    /* The field to sort by from the request. */
    @SerialName(value = "sortBy")
    val sortBy: kotlin.String? = null,

    /* Whether to sort in descending order. */
    @SerialName(value = "sortDesc")
    val sortDesc: kotlin.Boolean? = null,

    /* The field to filter by from the request. TODO */
    @SerialName(value = "filterBy")
    val filterBy: kotlin.String? = null,

    /* Return minified items if true. */
    @SerialName(value = "minified")
    val minified: kotlin.Boolean? = false,

    /* The fields to include in the response. The only current option is `rssfeed`. */
    @SerialName(value = "include")
    val include: kotlin.String? = null

) {


}

