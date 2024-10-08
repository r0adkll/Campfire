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
 * The email settings configuration for the server. This includes the credentials to send e-books and an array of e-reader devices.
 *
 * @param id The unique identifier for the email settings. Currently this is always `email-settings`
 * @param port The port number for the SMTP server.
 * @param secure Indicates if the connection should use SSL/TLS.
 * @param ereaderDevices List of configured e-reader devices.
 * @param host The SMTP host address.
 * @param rejectUnauthorized Indicates if unauthorized SSL/TLS certificates should be rejected.
 * @param user The username for SMTP authentication.
 * @param pass The password for SMTP authentication.
 * @param testAddress The test email address used for sending test emails.
 * @param fromAddress The default \"from\" email address for outgoing emails.
 */
@Serializable
data class EmailSettings(

  /* The unique identifier for the email settings. Currently this is always `email-settings` */
  @SerialName(value = "id")
  val id: kotlin.String,

  /* The port number for the SMTP server. */
  @SerialName(value = "port")
  val port: kotlin.Int,

  /* Indicates if the connection should use SSL/TLS. */
  @SerialName(value = "secure")
  val secure: kotlin.Boolean,

  /* List of configured e-reader devices. */
  @SerialName(value = "ereaderDevices")
  val ereaderDevices: kotlin.collections.List<EreaderDeviceObject>,

  /* The SMTP host address. */
  @SerialName(value = "host")
  val host: kotlin.String? = null,

  /* Indicates if unauthorized SSL/TLS certificates should be rejected. */
  @SerialName(value = "rejectUnauthorized")
  val rejectUnauthorized: kotlin.Boolean? = null,

  /* The username for SMTP authentication. */
  @SerialName(value = "user")
  val user: kotlin.String? = null,

  /* The password for SMTP authentication. */
  @SerialName(value = "pass")
  val pass: kotlin.String? = null,

  /* The test email address used for sending test emails. */
  @SerialName(value = "testAddress")
  val testAddress: kotlin.String? = null,

  /* The default \"from\" email address for outgoing emails. */
  @SerialName(value = "fromAddress")
  val fromAddress: kotlin.String? = null,

)
