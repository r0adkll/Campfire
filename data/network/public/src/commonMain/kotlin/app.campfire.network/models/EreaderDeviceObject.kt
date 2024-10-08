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
 * An e-reader device configured to receive EPUB through e-mail.
 *
 * @param name The name of the e-reader device.
 * @param email The email address associated with the e-reader device.
 * @param availabilityOption The availability option for the device.
 * @param users List of specific users allowed to access the device.
 */
@Serializable
data class EreaderDeviceObject(

  /* The name of the e-reader device. */
  @SerialName(value = "name")
  val name: kotlin.String,

  /* The email address associated with the e-reader device. */
  @SerialName(value = "email")
  val email: kotlin.String,

  /* The availability option for the device. */
  @SerialName(value = "availabilityOption")
  val availabilityOption: EreaderDeviceObject.AvailabilityOption,

  /* List of specific users allowed to access the device. */
  @SerialName(value = "users")
  val users: kotlin.collections.List<kotlin.String>? = null,

) {

  /**
   * The availability option for the device.
   *
   * Values: AdminOrUp,UserOrUp,GuestOrUp,SpecificUsers
   */
  @Serializable
  enum class AvailabilityOption(val value: kotlin.String) {
    @SerialName(value = "adminOrUp")
    AdminOrUp("adminOrUp"),

    @SerialName(value = "userOrUp")
    UserOrUp("userOrUp"),

    @SerialName(value = "guestOrUp")
    GuestOrUp("guestOrUp"),

    @SerialName(value = "specificUsers")
    SpecificUsers("specificUsers"),
  }
}
