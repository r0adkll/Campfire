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
 * The base book metadata object for minified, normal, and extended schemas to inherit from.
 *
 * @param title The title of the book. Will be null if unknown.
 * @param subtitle The subtitle of the book. Will be null if there is no subtitle.
 * @param genres The genres of the book.
 * @param publishedYear The year the book was published. Will be null if unknown.
 * @param publishedDate The date the book was published. Will be null if unknown.
 * @param publisher The publisher of the book. Will be null if unknown.
 * @param description A description for the book. Will be null if empty.
 * @param isbn The ISBN of the book. Will be null if unknown.
 * @param asin The ASIN of the book. Will be null if unknown.
 * @param language The language of the book. Will be null if unknown.
 * @param explicit Whether the book has been marked as explicit.
 */
@Serializable

data class BookMetadataBase (

    /* The title of the book. Will be null if unknown. */
    @SerialName(value = "title")
    val title: kotlin.String? = null,

    /* The subtitle of the book. Will be null if there is no subtitle. */
    @SerialName(value = "subtitle")
    val subtitle: kotlin.String? = null,

    /* The genres of the book. */
    @SerialName(value = "genres")
    val genres: kotlin.collections.List<kotlin.String>? = null,

    /* The year the book was published. Will be null if unknown. */
    @SerialName(value = "publishedYear")
    val publishedYear: kotlin.String? = null,

    /* The date the book was published. Will be null if unknown. */
    @SerialName(value = "publishedDate")
    val publishedDate: kotlin.String? = null,

    /* The publisher of the book. Will be null if unknown. */
    @SerialName(value = "publisher")
    val publisher: kotlin.String? = null,

    /* A description for the book. Will be null if empty. */
    @SerialName(value = "description")
    val description: kotlin.String? = null,

    /* The ISBN of the book. Will be null if unknown. */
    @SerialName(value = "isbn")
    val isbn: kotlin.String? = null,

    /* The ASIN of the book. Will be null if unknown. */
    @SerialName(value = "asin")
    val asin: kotlin.String? = null,

    /* The language of the book. Will be null if unknown. */
    @SerialName(value = "language")
    val language: kotlin.String? = null,

    /* Whether the book has been marked as explicit. */
    @SerialName(value = "explicit")
    val explicit: kotlin.Boolean? = null

) {


}

