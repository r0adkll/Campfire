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
 * @param name The name of the author.
 * @param description The new description of the author.
 * @param imagePath The absolute path for the author image. This will be in the `metadata/` directory. Will be null if there is no image.
 * @param asin The Audible identifier (ASIN) of the author. Will be null if unknown. Not the Amazon identifier.
 */
@Serializable

data class UpdateAuthorByIdRequest (

    /* The name of the author. */
    @SerialName(value = "name")
    val name: kotlin.String? = null,

    /* The new description of the author. */
    @SerialName(value = "description")
    val description: kotlin.String? = null,

    /* The absolute path for the author image. This will be in the `metadata/` directory. Will be null if there is no image. */
    @SerialName(value = "imagePath")
    val imagePath: kotlin.String? = null,

    /* The Audible identifier (ASIN) of the author. Will be null if unknown. Not the Amazon identifier. */
    @SerialName(value = "asin")
    val asin: kotlin.String? = null

) {


}

