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


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param include A comma separated list of what to include with the series.
 */
@Serializable

data class GetSeriesRequest (

    /* A comma separated list of what to include with the series. */
    @SerialName(value = "include")
    val include: GetSeriesRequest.Include? = null

) {

    /**
     * A comma separated list of what to include with the series.
     *
     * Values: Progress,Rssfeed,ProgressCommaRssfeed,RssfeedCommaProgress
     */
    @Serializable
    enum class Include(val value: kotlin.String) {
        @SerialName(value = "progress") Progress("progress"),
        @SerialName(value = "rssfeed") Rssfeed("rssfeed"),
        @SerialName(value = "progress,rssfeed") ProgressCommaRssfeed("progress,rssfeed"),
        @SerialName(value = "rssfeed,progress") RssfeedCommaProgress("rssfeed,progress");
    }

}

